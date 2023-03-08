package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.PointList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatchWithIsochrone;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatchWithIsochrone.CandidateMatchWithIsochrone;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
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

    private static final int RADIUS_TO_DIAMETER = 2;
    private static final int ALL_NODES = 3;
    private static final boolean INCLUDE_ELEVATION = false;
    private static final int NUM_POINTS = 100;
    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;

    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);
    private static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.RD_NEW_SRID);

    private final LinkFlagEncoder flagEncoder;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final QueryGraph queryGraph;
    private final IsochroneService isochroneService;
    private final BearingCalculator bearingCalculator;
    private final PointMatchingService pointMatchingService;
    private final CrsTransformer crsTransformer;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final NetworkGraphHopper network;

    public GraphHopperSinglePointMapMatcher(NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);
        this.network = network;

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        Weighting weighting = new ShortestWeighting(flagEncoder);
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        FractionAndDistanceCalculator fractionAndDistanceCalculator = new FractionAndDistanceCalculator(
                geodeticCalculator);
        this.edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        this.isochroneService = new IsochroneService(flagEncoder,
                edgeIteratorStateReverseExtractor,
                new IsochroneMatchMapper(new CrsTransformer(), flagEncoder,
                        fractionAndDistanceCalculator,
                        edgeIteratorStateReverseExtractor),
                new IsochroneFactory(weighting)
        );
        this.bearingCalculator = new BearingCalculator(geodeticCalculator);
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY,
                this.bearingCalculator,
                fractionAndDistanceCalculator);
        this.crsTransformer = new CrsTransformer();
    }
    /*  Todo: this should be integrated into the existing match method.
         For now only the first is with shortest distance.
     *  Proposal: introduce two smart enums. Filter and Sort in the SinglePointLocation
     *  with lambdas Filter.ALL (default) Filter.FIRST Sort.RELIABILITY (default) Sort.DISTANCE
     * */
    @Override
    public SinglePointMatchWithIsochrone matchWithIsochrone(SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        Point inputPoint = singlePointLocation.getPoint();
        double inputRadius = singlePointLocation.getCutoffDistance();
        List<QueryResult> queryResults = findCandidates(inputPoint, inputRadius);
        Polygon circle = createCircle(inputPoint, RADIUS_TO_DIAMETER * inputRadius);
        List<MatchedPoint> matches = queryResults.stream()
                .filter(qr -> intersects(circle, qr))
                .flatMap(qr -> calculateMatches(qr, circle, singlePointLocation)
                        .stream())
                .sorted(comparing(MatchedPoint::getDistance))
                .toList();
        if (matches.isEmpty()) {
            return createFailedMatchWithIsochrone(singlePointLocation);
        }
        MatchedPoint closestMatchedPoint = matches.get(0);
        List<IsochroneMatch> upstream =
                singlePointLocation.getUpstreamIsochroneUnit() == null ? Collections.emptyList() : isochroneService
                        .getUpstreamIsochroneMatches(closestMatchedPoint,
                                new QueryGraph(network.getGraphHopperStorage()), singlePointLocation,
                                locationIndexTree);
        List<IsochroneMatch> downstream =
                singlePointLocation.getDownstreamIsochroneUnit() == null ? Collections.emptyList() : isochroneService
                        .getDownstreamIsochroneMatches(closestMatchedPoint,
                                new QueryGraph(network.getGraphHopperStorage()), singlePointLocation,
                                locationIndexTree);
        CandidateMatchWithIsochrone candidateMatchWithIsochrone = CandidateMatchWithIsochrone
                .builder()
                .matchedLinkId(closestMatchedPoint.getMatchedLinkId())
                .reversed(closestMatchedPoint.isReversed())
                .upstream(upstream)
                .downstream(downstream)
                .snappedPoint(closestMatchedPoint.getSnappedPoint())
                .fraction(closestMatchedPoint.getFraction())
                .distance(closestMatchedPoint.getDistance())
                .reliability(calculateReliability(closestMatchedPoint, singlePointLocation))
                .build();
        return SinglePointMatchWithIsochrone
                .builder()
                .reliability(candidateMatchWithIsochrone.getReliability())
                .candidateMatches(List.of(candidateMatchWithIsochrone))
                .build();
    }


    @Override
    public SinglePointMatch match(SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        Point inputPoint = singlePointLocation.getPoint();
        double inputRadius = singlePointLocation.getCutoffDistance();
        List<QueryResult> queryResults = findCandidates(inputPoint, inputRadius);

        if (singlePointLocation.getUpstreamIsochroneUnit() != null ||
                singlePointLocation.getDownstreamIsochroneUnit() != null) {
            queryGraph.lookup(queryResults);
        }

        Polygon circle = createCircle(inputPoint, RADIUS_TO_DIAMETER * inputRadius);
        // Crop geometry to only include segments in search radius
        List<CandidateMatch> candidateMatches = queryResults.stream()
                .filter(qr -> intersects(circle, qr))
                .flatMap(qr -> createMatch(qr, circle, singlePointLocation))
                .sorted(comparingDouble(CandidateMatch::getReliability).reversed())
                .toList();
        if (candidateMatches.isEmpty()) {
            return createFailedMatch(singlePointLocation);
        }
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(candidateMatches)
                .reliability(candidateMatches.get(0).getReliability())
                .status(MatchStatus.MATCH)
                .build();
    }

    private boolean intersects(Polygon circle, QueryResult queryResult) {
        PointList pl = queryResult.getClosestEdge().fetchWayGeometry(ALL_NODES);
        return circle.intersects(pl.toLineString(INCLUDE_ELEVATION));
    }

    private Polygon createCircle(Point pointWgs84, double diameterInMeters) {
        var shapeFactory = new GeometricShapeFactory(RD_NEW_GEOMETRY_FACTORY);
        Point pointRd = (Point) crsTransformer.transformFromWgs84ToRdNew(pointWgs84);
        shapeFactory.setCentre(new Coordinate(pointRd.getX(), pointRd.getY()));
        shapeFactory.setNumPoints(NUM_POINTS);
        shapeFactory.setWidth(diameterInMeters);
        shapeFactory.setHeight(diameterInMeters);
        Polygon ellipseRd = shapeFactory.createEllipse();
        return (Polygon) crsTransformer.transformFromRdNewToWgs84(ellipseRd);
    }

    private List<QueryResult> findCandidates(Point point, double radius) {
        return getQueryResults(point, radius, locationIndexTree, edgeFilter);
    }

    private Stream<CandidateMatch> createMatch(QueryResult queryResult, Polygon circle,
            SinglePointLocation singlePointLocation) {
        int nodeId = queryResult.getClosestNode();
        Set<Integer> upstreamLinkIds = singlePointLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
        Set<Integer> downstreamLinkIds = singlePointLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
        return calculateMatches(queryResult, circle, singlePointLocation)
                .stream()
                .map(matchedPoint -> CandidateMatch.builder()
                        .matchedLinkId(matchedPoint.getMatchedLinkId())
                        .reversed(matchedPoint.isReversed())
                        .upstreamLinkIds(upstreamLinkIds)
                        .downstreamLinkIds(downstreamLinkIds)
                        .snappedPoint(matchedPoint.getSnappedPoint())
                        .fraction(matchedPoint.getFraction())
                        .distance(matchedPoint.getDistance())
                        .bearing(matchedPoint.getBearing())
                        .reliability(calculateReliability(matchedPoint, singlePointLocation))
                        .build());
    }

    private List<MatchedPoint> calculateMatches(QueryResult queryResult, Polygon circle,
            SinglePointLocation singlePointLocation) {
        LineString wayGeometry = queryResult.getClosestEdge()
                .fetchWayGeometry(ALL_NODES)
                .toLineString(false);
        /*
           The geometry direction of the edge iterator wayGeometry does not necessarily reflect the direction of a
           street or the original encoded geometry direction. It is just the direction of the exploration of the graph.
           GraphHopper sometimes reverses the geometry direction with respect to the original direction. To fix this,
           an internal attribute of the edge iterator state is used, indicating it has done so or not.
        */
        LineString originalGeometry =
                edgeIteratorStateReverseExtractor.hasReversed(queryResult.getClosestEdge()) ? wayGeometry.reverse()
                        : wayGeometry;
        Geometry cutoffGeometry = circle.intersection(originalGeometry);
        EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(queryResult, flagEncoder);
        IntsRef flags = queryResult.getClosestEdge().getFlags();
        int matchedLinkId = flagEncoder.getId(flags);
        var matchedQueryResult = MatchedQueryResult.builder()
                .matchedLinkId(matchedLinkId)
                .inputPoint(singlePointLocation.getPoint())
                .bearingFilter(singlePointLocation.getBearingFilter())
                .travelDirection(travelDirection)
                .originalGeometry(originalGeometry)
                .cutoffGeometry(cutoffGeometry)
                .build();

        return pointMatchingService.calculateMatches(matchedQueryResult);

    }

    private double calculateReliability(MatchedPoint matchedPoint, SinglePointLocation singlePointLocation) {
        double distancePenalty = matchedPoint.getDistance() / singlePointLocation.getCutoffDistance();
        double bearingPenalty = Optional.ofNullable(singlePointLocation.getBearingFilter())
                .map(bf -> bearingCalculator.bearingDelta(matchedPoint.getBearing(), bf.target()) / bf.cutoffMargin())
                .orElse(0.0);
        return Math.max(MIN_RELIABILITY_SCORE, (1 - distancePenalty - bearingPenalty) * MAX_RELIABILITY_SCORE);
    }

    private SinglePointMatch createFailedMatch(SinglePointLocation singlePointLocation) {
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }

    private SinglePointMatchWithIsochrone createFailedMatchWithIsochrone(SinglePointLocation singlePointLocation) {
        return SinglePointMatchWithIsochrone.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }
}
