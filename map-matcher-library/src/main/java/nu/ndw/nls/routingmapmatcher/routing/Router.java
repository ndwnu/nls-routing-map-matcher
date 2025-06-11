package nu.ndw.nls.routingmapmatcher.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FiniteWeightFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters.CH;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PathSimplification;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RamerDouglasPeucker;
import com.graphhopper.util.shapes.BBox;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.domain.AbstractMapMatcher;
import nu.ndw.nls.routingmapmatcher.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.mappers.MatchedLinkMapper;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedEdgeLink;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse.RoutingResponseBuilder;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.PathUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

public class Router extends AbstractMapMatcher {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int DECIMAL_PLACES = 3;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;
    private final MatchedLinkMapper matchedLinkMapper;
    private final GeometryFactoryWgs84 geometryFactoryWgs84;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;


    public Router(
            NetworkGraphHopper network,
            MatchedLinkMapper matchedLinkMapper,
            GeometryFactoryWgs84 geometryFactoryWgs84,
            FractionAndDistanceCalculator fractionAndDistanceCalculator, String profileName, CustomModel customModel
    ) {
        super(profileName, network, customModel);
        this.matchedLinkMapper = matchedLinkMapper;
        this.geometryFactoryWgs84 = geometryFactoryWgs84;
        this.fractionAndDistanceCalculator = fractionAndDistanceCalculator;
        // This configuration is global for the routing network and is probably not thread safe.
        // To be able to configure simplification per request, it's safer to disable GraphHopper-internal simplification
        // and perform it in our own response mapping code below.
        getNetwork().getRouterConfig().setSimplifyResponse(false);
    }

    public RoutingResponse route(RoutingRequest routingRequest) throws RoutingException, RoutingRequestException {
        List<Point> points = routingRequest.isSnapToNodes()
                ? snapPointsToNodes(routingRequest.getWayPoints())
                : routingRequest.getWayPoints();
        GHRequest graphHopperRequest = getGraphHopperRequest(points);
        if (getCustomModel() != null) {
            graphHopperRequest.setCustomModel(getCustomModel());
        }
        return getRoutingResponse(
                graphHopperRequest,
                routingRequest.isSimplifyResponseGeometry()
        );
    }

    private List<Point> snapPointsToNodes(List<Point> points) {
        ensurePointsAreInBounds(points);
        List<Point> snappedPoints = points.stream()
                .map(this::snapPointToNode)
                .distinct()
                .toList();
        if (snappedPoints.size() != points.size()) {
            throw new RoutingRequestException("Invalid routing request: Points are snapped to the same node");
        }
        return snappedPoints;
    }

    private void ensurePointsAreInBounds(List<Point> points) {
        BBox bounds = getNetwork().getBaseGraph().getBounds();
        for (Point point : points) {
            if (!bounds.contains(point.getY(), point.getX())) {
                throw new RoutingRequestException(
                        "Invalid routing request: Point is out of bounds: %s, the bounds are: %s".formatted(point, bounds)
                );
            }
        }
    }

    private Point snapPointToNode(Point point) {
        Weighting weighting = getNetwork().createWeighting(getProfile(), createCustomModelHintsIfPresent());
        FiniteWeightFilter finiteWeightFilter = new FiniteWeightFilter(weighting);
        Snap snap = getNetwork().getLocationIndex().findClosest(point.getY(), point.getX(), finiteWeightFilter);
        if (!snap.isValid()) {
            throw new RoutingRequestException(
                    "Invalid routing request: Cannot snap point %s,%s to node".formatted(point.getY(), point.getX())
            );
        }
        double snappedLat = getNetwork().getBaseGraph().getNodeAccess().getLat(snap.getClosestNode());
        double snappedLon = getNetwork().getBaseGraph().getNodeAccess().getLon(snap.getClosestNode());
        return geometryFactoryWgs84.createPoint(new Coordinate(snappedLon, snappedLat));
    }


    private RoutingResponse getRoutingResponse(GHRequest ghRequest, boolean simplify)
            throws RoutingRequestException, RoutingException {
        GHResponse ghResponse = getNetwork().route(ghRequest);
        ensureResponseHasNoErrors(ghResponse);
        ResponsePath responsePath = ghResponse.getBest();
        ensurePathsAreNotEmpty(responsePath);

        List<RoutingLegResponse> routingLegResponses = getRoutingLegResponses(ghRequest);
        return createRoutingResponse(responsePath, simplify)
                .legs(routingLegResponses)
                .build();
    }

    private GHRequest getGraphHopperRequest(List<Point> points) {
        GHRequest ghRequest = new GHRequest(getGHPointsFromPoints(points));
        ghRequest.setProfile(getProfile().getName());
        PMap snappedHints = ghRequest.getHints();
        snappedHints.putObject(Routing.CALC_POINTS, true);
        snappedHints.putObject(Routing.INSTRUCTIONS, false);
        snappedHints.putObject(CH.DISABLE, true);
        snappedHints.putObject(Routing.PASS_THROUGH, false);
        return ghRequest;
    }

    private List<RoutingLegResponse> getRoutingLegResponses(GHRequest ghRequest) throws RoutingException {
        EncodingManager encodingManager = getNetwork().getEncodingManager();
        List<RoutingLegResponse> routingLegResponse = new ArrayList<>();

        for (Path path : getNetwork().calcPaths(ghRequest)) {
            List<EdgeIteratorState> edges = path.calcEdges();
            if (edges.isEmpty()) {
                throw new RoutingException("Unexpected: path has no edges");
            }
            QueryGraph queryGraph = QueryGraphExtractor.extractQueryGraph(path);
            double startFraction = PathUtil.determineStartLinkFraction(edges.getFirst(), queryGraph, fractionAndDistanceCalculator);
            double endFraction = PathUtil.determineEndLinkFraction(edges.getLast(), queryGraph, fractionAndDistanceCalculator);
            List<MatchedEdgeLink> matchedEdgeLinks = PathUtil.determineMatchedLinks(encodingManager, fractionAndDistanceCalculator, edges);

            routingLegResponse.add(RoutingLegResponse.builder()
                    .matchedLinks(matchedLinkMapper.map(matchedEdgeLinks, startFraction, endFraction))
                    .build());
        }

        return routingLegResponse;
    }

    private RoutingResponseBuilder createRoutingResponse(ResponsePath path, boolean simplify) {
        PointList points = simplify ? PathSimplification.simplify(path, new RamerDouglasPeucker(), false) : path.getPoints();
        return RoutingResponse.builder()
                .geometry(points.toLineString(INCLUDE_ELEVATION))
                .snappedWaypoints(mapToSnappedWaypoints(path.getWaypoints()))
                .weight(Helper.round(path.getRouteWeight(), DECIMAL_PLACES))
                .duration(path.getTime() / MILLISECONDS_PER_SECOND)
                .distance(Helper.round(path.getDistance(), DECIMAL_PLACES));
    }

    private static List<GHPoint> getGHPointsFromPoints(List<Point> points) {
        return points.stream().map(point -> new GHPoint(point.getY(), point.getX())).toList();
    }

    private static void ensurePathsAreNotEmpty(ResponsePath path) throws RoutingException {
        if (path.getWaypoints().isEmpty()) {
            throw new RoutingException("Calculate resulted in no paths");
        }
    }

    private static void ensureResponseHasNoErrors(GHResponse ghResponse) throws RoutingRequestException {
        if (ghResponse.hasErrors()) {
            String errors = ghResponse.getErrors().stream().map(Throwable::getMessage)
                    .collect(Collectors.joining(", "));
            throw new RoutingRequestException("Invalid routing request: " + errors);
        }
    }

    private List<Point> mapToSnappedWaypoints(PointList pointList) {
        List<Point> waypoints = new ArrayList<>(pointList.size());
        for (int i = 0; i < pointList.size(); i++) {
            Point waypoint = geometryFactoryWgs84.createPoint(new Coordinate(pointList.getLon(i), pointList.getLat(i)));
            waypoints.add(waypoint);
        }
        return waypoints;
    }
}
