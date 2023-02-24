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
import com.graphhopper.util.PointList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
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

    private final QueryGraph queryGraph;
    private final IsochroneService isochroneService;
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
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.isochroneService = new IsochroneService(flagEncoder, weighting);
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY,
                new BearingCalculator(new GeodeticCalculator()),
                new FractionAndDistanceCalculator(new GeodeticCalculator()));
        this.crsTransformer = new CrsTransformer();
    }

    @Override
    public SinglePointMatch match(final SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        final Point inputPoint = singlePointLocation.getPoint();
        final double inputRadius = singlePointLocation.getCutoffDistance();
        final BearingFilter bearingFilter = singlePointLocation.getBearingFilter();
        final List<QueryResult> queryResults = findCandidates(inputPoint, inputRadius);

        if (singlePointLocation.getUpstreamIsochroneUnit() != null ||
                singlePointLocation.getDownstreamIsochroneUnit() != null) {
            queryGraph.lookup(queryResults);
        }

        final Polygon circle = createCircle(inputPoint, 2 * inputRadius);
        // Crop geometry to only include segments in search radius
        final List<CandidateMatch> candidateMatches = queryResults.stream()
                .filter(qr -> intersects(circle, qr))
                .flatMap(qr -> createMatch(qr, circle, singlePointLocation, inputPoint, bearingFilter))
                .sorted(comparing(CandidateMatch::getDistance))
                .collect(Collectors.toList());
        if (candidateMatches.isEmpty()) {
            return createFailedMatch(singlePointLocation);
        }
        final double closestDistance = candidateMatches.get(0).getDistance();
        final double reliability = calculateReliability(closestDistance, inputRadius);
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(candidateMatches)
                .reliability(reliability)
                .status(MatchStatus.MATCH)
                .build();
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

    private Stream<CandidateMatch> createMatch(final QueryResult queryResult, final Polygon circle,
            final SinglePointLocation singlePointLocation, final Point inputPoint, final BearingFilter bearingFilter) {
        final LineString wayGeometry = queryResult.getClosestEdge()
                .fetchWayGeometry(ALL_NODES)
                .toLineString(false);
        /*
           The geometry direction of the edge iterator wayGeometry does not necessarily reflect the direction of a
           street or the original encoded geometry direction. It is just the direction of the exploration of the graph.
           GraphHopper sometimes reverses the geometry direction with respect to the original direction. To fix this,
           an internal attribute of the edge iterator state is used, indicating it has done so or not.
        */
        final LineString originalGeometry = hasReversed(queryResult) ? wayGeometry.reverse() : wayGeometry;
        final Geometry cutoffGeometry = circle.intersection(originalGeometry);
        final EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(queryResult, flagEncoder);
        final IntsRef flags = queryResult.getClosestEdge().getFlags();
        final int matchedLinkId = flagEncoder.getId(flags);
        final int nodeId = queryResult.getClosestNode();
        final Set<Integer> upstreamLinkIds = singlePointLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
        final Set<Integer> downstreamLinkIds = singlePointLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
        final var matchedQueryResult = MatchedQueryResult.builder()
                .matchedLinkId(matchedLinkId)
                .inputPoint(inputPoint)
                .bearingFilter(bearingFilter)
                .travelDirection(travelDirection)
                .originalGeometry(originalGeometry)
                .cutoffGeometry(cutoffGeometry)
                .build();
        return pointMatchingService.calculateMatches(matchedQueryResult).stream()
                .map(matchedLineSegment -> CandidateMatch.builder()
                        .matchedLinkId(matchedLineSegment.getMatchedLinkId())
                        .upstreamLinkIds(upstreamLinkIds)
                        .downstreamLinkIds(downstreamLinkIds)
                        .snappedPoint(matchedLineSegment.getSnappedPoint())
                        .fraction(matchedLineSegment.getFractionOfSnappedPoint())
                        .distance(matchedLineSegment.getDistanceToSnappedPoint())
                        .bearing(matchedLineSegment.getBearingOfSnappedPoint())
                        .reversed(matchedLineSegment.isReversed())
                        .build());
    }

    private static double calculateReliability(final double closestDistance, final double radius) {
        return (1 - closestDistance / radius) * MAX_RELIABILITY_SCORE;
    }

    private SinglePointMatch createFailedMatch(final SinglePointLocation singlePointLocation) {
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }
}
