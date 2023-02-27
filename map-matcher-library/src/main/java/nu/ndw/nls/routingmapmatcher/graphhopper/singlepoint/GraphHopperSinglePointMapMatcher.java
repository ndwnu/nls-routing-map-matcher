package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static com.graphhopper.storage.EdgeIteratorStateReverseExtractor.hasReversed;
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
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocationWithBearing;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;

public class GraphHopperSinglePointMapMatcher implements SinglePointMapMatcher {

    /**
     * Only search for candidates within this distance.
     */
    private static final double DEFAULT_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    /**
     * Distances returned by GraphHopper contain floating point errors compared to the source data, so a delta needs to
     * be used when comparing distances to find all segments that are equally far in the source data.
     */
    private static final double DISTANCE_ROUNDING_ERROR = 0.1;

    private static final int NUM_POINTS = 100;
    private static final int MAX_RELIABILITY_SCORE = 100;

    private static final int ALL_NODES = 3;
    private static final boolean INCLUDE_ELEVATION = false;
    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);
    private static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.RD_NEW_SRID);

    private final LinkFlagEncoder flagEncoder;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final PathUtil pathUtil;
    private final QueryGraph queryGraph;
    private final IsochroneService isochroneService;
    private final DistanceCalc distanceCalculator;
    private final PointMatchingService pointMatchingService;
    private final CrsTransformer crsTransformer;

    public GraphHopperSinglePointMapMatcher(final NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        final List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        this.pathUtil = new PathUtil(WGS84_GEOMETRY_FACTORY);
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.isochroneService = new IsochroneService(flagEncoder, weighting);
        this.distanceCalculator = new DistanceCalcEarth();
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY,
                new BearingCalculator(new GeodeticCalculator()),
                new FractionAndDistanceCalculator(new GeodeticCalculator()));
        this.crsTransformer = new CrsTransformer();
    }

    @Override
    public SinglePointMatch match(final SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        final double inputRadius = singlePointLocation.getRadius() != null ?
                singlePointLocation.getRadius()
                : DEFAULT_CANDIDATE_DISTANCE_IN_METERS;
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
    public SinglePointMatch matchWithBearing(final SinglePointLocationWithBearing singlePointLocationWithBearing) {
        Preconditions.checkNotNull(singlePointLocationWithBearing);
        final Point inputPoint = singlePointLocationWithBearing.getPoint();
        final double inputRadius = singlePointLocationWithBearing.getRadius() != null ?
                singlePointLocationWithBearing.getRadius()
                : DEFAULT_CANDIDATE_DISTANCE_IN_METERS;
        final BearingRange bearingRange = singlePointLocationWithBearing.getBearingRange();
        final List<QueryResult> result = findCandidates(inputPoint, inputRadius);
        final Polygon circle = createCircle(inputPoint, 2 * inputRadius);
        // Crop geometry to only include segments in search radius
        final List<MatchedPoint> filteredResults = result.stream()
                // filter on intersects
                .filter(qr -> intersects(circle, qr))
                .map(q -> {
                    final LineString wayGeometry = q.getClosestEdge()
                            .fetchWayGeometry(ALL_NODES)
                            .toLineString(false);
                    /*
                       The geometry direction of the edge iterator wayGeometry does not necessarily reflect the
                       direction of a street or the original encoded geometry direction. It is just the direction of
                       the exploration of the graph. GraphHopper sometimes reverses the geometry direction with respect
                       to the original direction. To fix this, an internal attribute of the edge iterator state is used,
                       indicating it has done so or not.
                    */
                    final LineString originalGeometry = hasReversed(q) ? wayGeometry.reverse() : wayGeometry;
                    final Geometry cutoffGeometry = circle.intersection(originalGeometry);
                    final EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(q, flagEncoder);
                    final IntsRef flags = q.getClosestEdge().getFlags();
                    final int matchedLinkId = flagEncoder.getId(flags);
                    return pointMatchingService.calculateMatches(MatchedQueryResult
                            .builder()
                            .matchedLinkId(matchedLinkId)
                            .inputPoint(inputPoint)
                            .bearingRange(bearingRange)
                            .travelDirection(travelDirection)
                            .originalGeometry(originalGeometry)
                            .cutoffGeometry(cutoffGeometry)
                            .build());
                })
                .flatMap(Collection::stream)
                .sorted(comparing(MatchedPoint::getDistanceToSnappedPoint))
                .toList();
        if (filteredResults.isEmpty()) {
            return createFailedMatch(singlePointLocationWithBearing);
        }
        final List<CandidateMatch> candidateMatches = filteredResults
                .stream()
                .map(matchedLineSegment -> new SinglePointMatch.CandidateMatch(
                        matchedLineSegment.getMatchedLinkId(),
                        null,
                        null,
                        matchedLineSegment.getSnappedPoint(),
                        matchedLineSegment.getFractionOfSnappedPoint(),
                        matchedLineSegment.getDistanceToSnappedPoint(),
                        matchedLineSegment.getBearingOfSnappedPoint(),
                        matchedLineSegment.isReversed()))
                .collect(Collectors.toList());
        final double closestDistance = filteredResults.stream()
                .mapToDouble(MatchedPoint::getDistanceToSnappedPoint).min()
                .orElse(DEFAULT_CANDIDATE_DISTANCE_IN_METERS);
        final double reliability = calculateReliability(closestDistance, inputRadius);
        return new SinglePointMatch(singlePointLocationWithBearing.getId(),
                candidateMatches,
                reliability, MatchStatus.MATCH);
    }

    private boolean intersects(final Polygon circle, final QueryResult queryResult) {
        final PointList pl = queryResult.getClosestEdge().fetchWayGeometry(ALL_NODES);
        return circle.intersects(pl.toLineString(INCLUDE_ELEVATION));
    }

    private Polygon createCircle(final Point pointWgs84, final double diameterInMeters) {
        final var shapeFactory = new GeometricShapeFactory(RD_NEW_GEOMETRY_FACTORY);
        final Point pointRd = (Point) crsTransformer.transformFromWgs84ToRdNew(pointWgs84);
        shapeFactory.setCentre(new Coordinate(pointRd.getX(), pointRd.getY()));
        shapeFactory.setNumPoints(NUM_POINTS);
        shapeFactory.setWidth(diameterInMeters);
        shapeFactory.setHeight(diameterInMeters);
        final Polygon ellipseRd = shapeFactory.createEllipse();
        return (Polygon) crsTransformer.transformFromRdNewToWgs84(ellipseRd);
    }

    private List<QueryResult> findCandidates(final Point point, final double radius) {
        return getQueryResults(point, radius, locationIndexTree, edgeFilter);
    }

    private SinglePointMatch createMatch(final List<QueryResult> queryResults,
            final SinglePointLocation singlePointLocation, final double inputRadius) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final double closestDistance = queryResults.stream().mapToDouble(QueryResult::getQueryDistance).min()
                .orElse(DEFAULT_CANDIDATE_DISTANCE_IN_METERS);

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
                final Point snappedPoint = WGS84_GEOMETRY_FACTORY.createPoint(
                        new Coordinate(ghSnappedPoint.getLon(), ghSnappedPoint.getLat()));

                final double fraction = this.pathUtil.determineSnappedPointFraction(queryResult,
                        this.distanceCalculator, flagEncoder);

                candidateMatches.add(new SinglePointMatch.CandidateMatch(matchedLinkId, upstreamLinkIds,
                        downstreamLinkIds, snappedPoint, fraction, queryResult.getQueryDistance(), null, false));
            }
        }

        final double reliability = calculateReliability(closestDistance, inputRadius);
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, MatchStatus.MATCH);
    }

    private static double calculateReliability(final double closestDistance, final double radius) {
        return (1 - closestDistance / radius) * MAX_RELIABILITY_SCORE;
    }

    private SinglePointMatch createFailedMatch(final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final MatchStatus matchStatus = MatchStatus.NO_MATCH;
        final double reliability = 0.0;
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, matchStatus);
    }
}
