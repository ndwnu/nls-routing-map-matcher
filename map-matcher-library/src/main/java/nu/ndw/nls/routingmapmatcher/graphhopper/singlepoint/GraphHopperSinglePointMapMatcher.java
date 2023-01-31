package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static java.util.Comparator.comparing;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocationWithBearing;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.TravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;

public class GraphHopperSinglePointMapMatcher implements SinglePointMapMatcher {

    /**
     * Only search for candidates within this distance.
     */
    private static final double DEFAULT_MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    /**
     * Distances returned by GraphHopper contain floating point errors compared to the source data, so a delta needs to
     * be used when comparing distances to find all segments that are equally far in the source data.
     */
    private static final double DISTANCE_ROUNDING_ERROR = 0.1;

    private static final int NUM_POINTS = 100;
    private static final int MAX_RELIABILITY_SCORE = 100;

    private static final double DEGREE_LATITUDE_IN_KM = 111320D;
    private static final int ALL_NODES = 3;
    private static final boolean INCLUDE_ELEVATION = false;
    private static final int EARTH_CIRCUMFERENCE = 40075000;
    private static final int CIRCLE_DEGREES = 360;
    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);

    private final LinkFlagEncoder flagEncoder;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;
    private final GeometryFactory geometryFactory;
    private final PathUtil pathUtil;
    private final QueryGraph queryGraph;
    private final IsochroneService isochroneService;
    private final DistanceCalc distanceCalculator;
    private final PointMatchingService pointMatchingService;

    public GraphHopperSinglePointMapMatcher(final NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        final List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        this.geometryFactory = WGS84_GEOMETRY_FACTORY;
        this.pathUtil = new PathUtil(this.geometryFactory);
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.isochroneService = new IsochroneService(flagEncoder, weighting);
        this.distanceCalculator = new DistanceCalcEarth();
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY, flagEncoder,
                new GeodeticCalculator());

    }

    @Override
    public SinglePointMatch match(final SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        final double inputRadius = singlePointLocation.getRadius() != null ?
                singlePointLocation.getRadius()
                : DEFAULT_MAXIMUM_CANDIDATE_DISTANCE_IN_METERS;
        final List<QueryResult> queryResults = findCandidates(singlePointLocation.getPoint(), inputRadius);
        final SinglePointMatch match;
        if (queryResults.isEmpty()) {
            match = createFailedMatch(singlePointLocation);
        } else {
            match = createMatch(queryResults, singlePointLocation, inputRadius);
        }

        return match;
    }

    @Override
    public SinglePointMatch matchWithBearing(SinglePointLocationWithBearing singlePointLocationWithBearing) {
        Preconditions.checkNotNull(singlePointLocationWithBearing);
        final Point inputPoint = singlePointLocationWithBearing.getPoint();
        final double inputRadius = singlePointLocationWithBearing.getRadius() != null ?
                singlePointLocationWithBearing.getRadius()
                : DEFAULT_MAXIMUM_CANDIDATE_DISTANCE_IN_METERS;
        final Double inputMinBearing = singlePointLocationWithBearing.getMinBearing();
        final Double inputMaxBearing = singlePointLocationWithBearing.getMaxBearing();
        final List<QueryResult> result = findCandidates(inputPoint, inputRadius);
        final Polygon circle = createCircle(inputPoint, inputRadius);
        // Crop geometry to only include segments in search radius
        final List<MatchedPoint> filteredResults = result.stream()
                // filter on intersects
                .filter(qr -> intersects(circle, qr))
                .map(q -> {
                    PointList pl = q.getClosestEdge().fetchWayGeometry(ALL_NODES);
                    Geometry cutoffGeometry = circle.intersection(pl.toLineString(INCLUDE_ELEVATION));
                    TravelDirection travelDirection = determineEdgeDirection(q, flagEncoder);
                    return pointMatchingService.calculateMatches(MatchedQueryResult
                            .builder()
                            .inputPoint(inputPoint)
                            .inputMinBearing(inputMinBearing)
                            .inputMaxBearing(inputMaxBearing)
                            .travelDirection(travelDirection)
                            .cutoffGeometry(cutoffGeometry)
                            .queryResult(q)
                            .build());
                })
                .flatMap(Collection::stream)
                .sorted(comparing(MatchedPoint::getDistanceToSnappedPoint))
                .collect(Collectors.toList());
        if (filteredResults.isEmpty()) {
            return createFailedMatch(singlePointLocationWithBearing);
        }
        List<CandidateMatch> candidateMatches = filteredResults
                .stream()
                .map(matchedLineSegment -> new SinglePointMatch.CandidateMatch(
                        matchedLineSegment.getMatchedLinkId(),
                        null,
                        null,
                        matchedLineSegment.getSnappedPoint(),
                        matchedLineSegment.getFractionOfSnappedPoint(),
                        matchedLineSegment.getDistanceToSnappedPoint()))
                .collect(Collectors.toList());
        final double closestDistance = filteredResults.stream()
                .mapToDouble(MatchedPoint::getDistanceToSnappedPoint).min()
                .orElse(DEFAULT_MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);
        final double reliability = calculateReliability(closestDistance, inputRadius);
        return new SinglePointMatch(singlePointLocationWithBearing.getId(),
                candidateMatches,
                reliability, MatchStatus.MATCH);
    }

    private boolean intersects(Polygon circle, QueryResult queryResult) {
        PointList pl = queryResult.getClosestEdge().fetchWayGeometry(ALL_NODES);
        return circle.intersects(pl.toLineString(INCLUDE_ELEVATION));
    }

    private Polygon createCircle(Point point, double distanceInMeters) {
        var shapeFactory = new GeometricShapeFactory(WGS84_GEOMETRY_FACTORY);
        shapeFactory.setCentre(new Coordinate(point.getX(), point.getY()));
        shapeFactory.setNumPoints(NUM_POINTS);
        shapeFactory.setWidth(distanceInMeters / DEGREE_LATITUDE_IN_KM);
        shapeFactory.setHeight(
                distanceInMeters / (EARTH_CIRCUMFERENCE * Math.cos(Math.toRadians(point.getX())) / CIRCLE_DEGREES));
        return shapeFactory.createEllipse();
    }

    private List<QueryResult> findCandidates(final Point point, double radius) {
        return getQueryResults(point, radius, locationIndexTree, edgeFilter);
    }


    private SinglePointMatch createMatch(final List<QueryResult> queryResults,
            final SinglePointLocation singlePointLocation, double inputRadius) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final double closestDistance = queryResults.stream().mapToDouble(QueryResult::getQueryDistance).min()
                .orElse(DEFAULT_MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);

        if (singlePointLocation.getUpstreamIsochroneUnit() != null ||
                singlePointLocation.getDownstreamIsochroneUnit() != null) {
            queryGraph.lookup(queryResults);
        }

        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() < closestDistance + DISTANCE_ROUNDING_ERROR) {
                final IntsRef flags = queryResult.getClosestEdge().getFlags();
                final int matchedLinkId = flagEncoder.getId(flags);
                final int nodeId = queryResult.getClosestNode();
                final Set<Integer> upstreamLinkIds = singlePointLocation.getUpstreamIsochroneUnit() != null ?
                        isochroneService.getUpstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
                final Set<Integer> downstreamLinkIds = singlePointLocation.getDownstreamIsochroneUnit() != null ?
                        isochroneService.getDownstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;

                final GHPoint3D ghSnappedPoint = queryResult.getSnappedPoint();
                final Point snappedPoint = geometryFactory.createPoint(
                        new Coordinate(ghSnappedPoint.getLon(), ghSnappedPoint.getLat()));

                final double fraction = this.pathUtil.determineSnappedPointFraction(queryResult,
                        this.distanceCalculator, flagEncoder);

                candidateMatches.add(new SinglePointMatch.CandidateMatch(matchedLinkId, upstreamLinkIds,
                        downstreamLinkIds, snappedPoint, fraction, queryResult.getQueryDistance()));
            }
        }

        final double reliability = calculateReliability(closestDistance, inputRadius);
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, MatchStatus.MATCH);
    }

    private static double calculateReliability(double closestDistance, double radius) {
        return (1 - closestDistance / radius) * MAX_RELIABILITY_SCORE;
    }

    private SinglePointMatch createFailedMatch(final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final MatchStatus matchStatus = MatchStatus.NO_MATCH;
        final double reliability = 0.0;
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, matchStatus);
    }
}
