package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchFilter.ALL;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static nu.ndw.nls.routingmapmatcher.util.Constants.SHORTEST_CUSTOM_MODEL;
import static nu.ndw.nls.routingmapmatcher.util.MatchUtil.getQueryResults;
import static nu.ndw.nls.routingmapmatcher.util.PathUtil.determineEdgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FiniteWeightFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PMap;
import java.util.List;
import nu.ndw.nls.geometry.bearing.BearingCalculator;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.geometry.mappers.DiameterToPolygonMapper;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcher;
import nu.ndw.nls.routingmapmatcher.geometry.services.ClosestPointService;
import nu.ndw.nls.routingmapmatcher.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class SinglePointMapMatcher implements MapMatcher<SinglePointLocation, SinglePointMatch> {

    private static final int RADIUS_TO_DIAMETER = 2;
    private static final double DISTANCE_THRESHOLD = 0.1;
    private static final double RELIABILITY_THRESHOLD = 0.5;

    private final LocationIndexTree locationIndexTree;
    private final IsochroneService isochroneService;
    private final PointMatchingService pointMatchingService;
    private final DiameterToPolygonMapper diameterToPolygonMapper;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final NetworkGraphHopper network;
    private final Profile profile;
    private final PointListUtil pointListUtil;

    public SinglePointMapMatcher(DiameterToPolygonMapper diameterToPolygonMapper,
            BearingCalculator bearingCalculator, GeometryFactoryWgs84 geometryFactoryWgs84,
            FractionAndDistanceCalculator fractionAndDistanceCalculator, NetworkGraphHopper network,
            String profileName, ClosestPointService closestPointService) {
        this.diameterToPolygonMapper = diameterToPolygonMapper;
        this.network = Preconditions.checkNotNull(network);
        this.profile = Preconditions.checkNotNull(network.getProfile(profileName));
        this.locationIndexTree = network.getLocationIndex();
        BaseGraph baseGraph = network.getBaseGraph();
        EncodingManager encodingManager = network.getEncodingManager();
        Weighting shortestWeightingForIsochrone = network.createWeighting(profile, createShortestDistanceHints());
        this.edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        this.pointListUtil = new PointListUtil();
        this.isochroneService = new IsochroneService(encodingManager, baseGraph, edgeIteratorStateReverseExtractor,
                new IsochroneMatchMapper(encodingManager, edgeIteratorStateReverseExtractor,
                        pointListUtil,
                        fractionAndDistanceCalculator),
                new ShortestPathTreeFactory(shortestWeightingForIsochrone), this.locationIndexTree, profile);
        this.pointMatchingService = new PointMatchingService(geometryFactoryWgs84, bearingCalculator,
                fractionAndDistanceCalculator, closestPointService);

    }

    private PMap createShortestDistanceHints() {
        return new PMap()
                .putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }

    public SinglePointMatch match(SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        Weighting matchWeighting = network.createWeighting(profile, new PMap());
        Point inputPoint = singlePointLocation.getPoint();
        double inputRadius = singlePointLocation.getCutoffDistance();
        List<Snap> queryResults = getQueryResults(network, inputPoint, inputRadius, locationIndexTree,
                new FiniteWeightFilter(matchWeighting));
        Polygon circle = diameterToPolygonMapper.mapToPolygonWgs84(inputPoint, RADIUS_TO_DIAMETER * inputRadius);
        List<MatchedPoint> matches = getMatchedPoints(singlePointLocation, queryResults, circle);
        if (matches.isEmpty()) {
            return createFailedMatch(singlePointLocation);
        }
        List<CandidateMatch> candidateMatches = matches.stream()
                .map(matchedPoint -> mapToCandidateMatch(singlePointLocation, matchedPoint))
                .toList();

        return SinglePointMatch
                .builder()
                .id(singlePointLocation.getId())
                .reliability(candidateMatches.getFirst().getReliability())
                .candidateMatches(candidateMatches)
                .status(MatchStatus.MATCH)
                .build();
    }

    private CandidateMatch mapToCandidateMatch(SinglePointLocation singlePointLocation, MatchedPoint matchedPoint) {
        List<IsochroneMatch> upstream = singlePointLocation.getUpstreamIsochroneUnit() == null ? null
                : isochroneService.getUpstreamIsochroneMatches(matchedPoint.getSnappedPoint(),
                        matchedPoint.getMatchedLinkId(), matchedPoint.isReversed(), singlePointLocation);
        List<IsochroneMatch> downstream = singlePointLocation.getDownstreamIsochroneUnit() == null ? null
                : isochroneService.getDownstreamIsochroneMatches(matchedPoint.getSnappedPoint(),
                        matchedPoint.getMatchedLinkId(), matchedPoint.isReversed(), singlePointLocation);

        return CandidateMatch
                .builder()
                .matchedLinkId(matchedPoint.getMatchedLinkId())
                .reversed(matchedPoint.isReversed())
                .upstream(upstream)
                .downstream(downstream)
                .snappedPoint(matchedPoint.getSnappedPoint())
                .fraction(matchedPoint.getFraction())
                .distance(matchedPoint.getDistance())
                .bearing(matchedPoint.getBearing())
                .reliability(matchedPoint.getReliability())
                .build();
    }

    private List<MatchedPoint> getMatchedPoints(SinglePointLocation singlePointLocation, List<Snap> queryResults,
            Polygon circle) {
        List<MatchedPoint> sorted = queryResults.stream()
                .map(Snap::getClosestEdge)
                .filter(e -> intersects(circle, e))
                .flatMap(e -> calculateMatches(e, circle, singlePointLocation)
                        .stream())
                .sorted(singlePointLocation.getMatchSort().getSort())
                .toList();
        if (sorted.isEmpty() || singlePointLocation.getMatchFilter() == ALL) {
            return sorted;
        } else {
            return switch (singlePointLocation.getMatchSort()) {
                case HIGHEST_RELIABILITY -> getMostReliable(sorted);
                case SHORTEST_DISTANCE -> getShortest(sorted);
            };
        }
    }

    private List<MatchedPoint> getShortest(List<MatchedPoint> sorted) {
        double cutoffValue = sorted.getFirst().getDistance() + DISTANCE_THRESHOLD;
        return sorted
                .stream()
                .filter(matchedPoint -> matchedPoint.getDistance() < cutoffValue)
                .toList();
    }

    private List<MatchedPoint> getMostReliable(List<MatchedPoint> sorted) {
        double cutoffValue = sorted.getFirst().getReliability() - RELIABILITY_THRESHOLD;
        return sorted
                .stream()
                .filter(matchedPoint -> matchedPoint.getReliability() > cutoffValue)
                .toList();
    }

    private boolean intersects(Polygon circle, EdgeIteratorState edge) {
        LineString wayGeometry = pointListUtil.toLineString(edge.fetchWayGeometry(FetchMode.ALL));
        return circle.intersects(wayGeometry);
    }

    private List<MatchedPoint> calculateMatches(EdgeIteratorState edge, Polygon circle,
            SinglePointLocation singlePointLocation) {
        LineString wayGeometry = pointListUtil.toLineString(edge.fetchWayGeometry(FetchMode.ALL));
        /*
           The geometry direction of the edge iterator wayGeometry does not necessarily reflect the direction of a
           street or the original encoded geometry direction. It is just the traversal direction within the graph.
           GraphHopper sometimes reverses the geometry direction with respect to the original direction. To fix this,
           an internal attribute of the edge iterator state is used, indicating it has done so or not.
        */
        LineString originalGeometry =
                edgeIteratorStateReverseExtractor.hasReversed(edge) ? wayGeometry.reverse() : wayGeometry;
        Geometry cutoffGeometry = circle.intersection(originalGeometry);
        EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(edge, network.getEncodingManager(),
                profile.getName());
        int matchedLinkId = edge.get(network.getEncodingManager().getIntEncodedValue(WAY_ID_KEY));
        var matchedQueryResult = MatchedQueryResult.builder()
                .matchedLinkId(matchedLinkId)
                .inputPoint(singlePointLocation.getPoint())
                .cutoffDistance(singlePointLocation.getCutoffDistance())
                .bearingFilter(singlePointLocation.getBearingFilter())
                .travelDirection(travelDirection)
                .originalGeometry(originalGeometry)
                .cutoffGeometry(cutoffGeometry)
                .build();

        return pointMatchingService.calculateMatches(matchedQueryResult);
    }

    private SinglePointMatch createFailedMatch(SinglePointLocation singlePointLocation) {
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }
}
