package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static java.util.Comparator.comparing;
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
import java.util.ArrayList;
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
    private static final double MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    /**
     * Distances returned by GraphHopper contain floating point errors compared to the source data, so a delta needs to
     * be used when comparing distances to find all segments that are equally far in the source data.
     */
    private static final double DISTANCE_ROUNDING_ERROR = 0.1;

    private static final int NUM_POINTS = 100;
    private static final int MAX_RELIABILITY_SCORE = 100;

    // Length in meters of 1° of latitude = always 111.32 km
    public static final double DEGREE_LATITUDE_IN_KM = 111320d;
    public static final int ALL_NODES = 3;
    public static final boolean INCLUDE_ELEVATION = false;

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
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(this.geometryFactory);
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.isochroneService = new IsochroneService(flagEncoder, weighting);
        this.distanceCalculator = new DistanceCalcEarth();
        this.pointMatchingService = new PointMatchingService(new GeometryFactory(new PrecisionModel(),
                GlobalConstants.WGS84_SRID), flagEncoder, new GeodeticCalculator());

    }

    @Override
    public SinglePointMatch match(final SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);

        Double inputRadius = singlePointLocation.getRadius() != null ?
                singlePointLocation.getRadius()
                : MAXIMUM_CANDIDATE_DISTANCE_IN_METERS;

        final List<QueryResult> queryResults = findCandidates(singlePointLocation.getPoint(), inputRadius);

        final SinglePointMatch match;
        if (queryResults.isEmpty()) {
            match = createFailedMatch(singlePointLocation);
        } else {
            match = createMatch(queryResults, singlePointLocation);
        }

        return match;
    }

    @Override
    public SinglePointMatch matchWithBearing(SinglePointLocationWithBearing singlePointLocationWithBearing) {
        Preconditions.checkNotNull(singlePointLocationWithBearing);

        Point inputPoint = singlePointLocationWithBearing.getPoint();
        Double inputRadius = singlePointLocationWithBearing.getRadius() != null ?
                singlePointLocationWithBearing.getRadius()
                : MAXIMUM_CANDIDATE_DISTANCE_IN_METERS;
        Double inputMinBearing = singlePointLocationWithBearing.getMinBearing();
        Double inputMaxBearing = singlePointLocationWithBearing.getMaxBearing();

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
                .orElse(MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);
        final double reliability = calculateReliability(closestDistance);
        return new SinglePointMatch(singlePointLocationWithBearing.getId(),
                candidateMatches,
                reliability, MatchStatus.MATCH);
    }

    private boolean intersects(Polygon circle, QueryResult queryResult) {
        PointList pl = queryResult.getClosestEdge().fetchWayGeometry(ALL_NODES);
        return circle.intersects(pl.toLineString(INCLUDE_ELEVATION));
    }

    private Polygon createCircle(Point point, double distanceInMeters) {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        var shapeFactory = new GeometricShapeFactory(gf);
        shapeFactory.setCentre(new Coordinate(point.getX(), point.getY()));
        shapeFactory.setNumPoints(NUM_POINTS);
        shapeFactory.setWidth(distanceInMeters / DEGREE_LATITUDE_IN_KM);
        shapeFactory.setHeight(distanceInMeters / (40075000 * Math.cos(Math.toRadians(point.getX())) / 360));
        return shapeFactory.createEllipse();
    }

    private List<QueryResult> findCandidates(final Point point, double radius) {
        final double latitude = point.getY();
        final double longitude = point.getX();

        final List<QueryResult> queryResults = locationIndexTree.findNClosest(latitude, longitude, edgeFilter,
                radius);
        final List<QueryResult> candidates = new ArrayList<>(queryResults.size());

        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= radius) {
                candidates.add(queryResult);
            }
        }

        return candidates;
    }

    private SinglePointMatch createMatch(final List<QueryResult> queryResults,
            final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final double closestDistance = queryResults.stream().mapToDouble(QueryResult::getQueryDistance).min()
                .orElse(MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);

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
                        downstreamLinkIds, snappedPoint, fraction, null));
            }
        }

        final double reliability = calculateReliability(closestDistance);
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, MatchStatus.MATCH);
    }

    private static double calculateReliability(double closestDistance) {
        return (1 - closestDistance / MAXIMUM_CANDIDATE_DISTANCE_IN_METERS) * MAX_RELIABILITY_SCORE;
    }

    private SinglePointMatch createFailedMatch(final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final MatchStatus matchStatus = MatchStatus.NO_MATCH;
        final double reliability = 0.0;
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, matchStatus);
    }
}
