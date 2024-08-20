package nu.ndw.nls.routingmapmatcher.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters.CH;
import com.graphhopper.util.Parameters.Routing;
import com.graphhopper.util.PathSimplification;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RamerDouglasPeucker;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
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

public class Router {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int DECIMAL_PLACES = 3;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final NetworkGraphHopper networkGraphHopper;

    private final MatchedLinkMapper matchedLinkMapper;
    private final GeometryFactoryWgs84 geometryFactoryWgs84;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    public Router(NetworkGraphHopper networkGraphHopper, MatchedLinkMapper matchedLinkMapper,
            GeometryFactoryWgs84 geometryFactoryWgs84, FractionAndDistanceCalculator fractionAndDistanceCalculator) {
        this.networkGraphHopper = networkGraphHopper;
        this.matchedLinkMapper = matchedLinkMapper;
        this.geometryFactoryWgs84 = geometryFactoryWgs84;
        this.fractionAndDistanceCalculator = fractionAndDistanceCalculator;
        // This configuration is global for the routing network and is probably not thread safe.
        // To be able to configure simplification per request, it's safer to disable GraphHopper-internal simplification
        // and perform it in our own response mapping code below.
        this.networkGraphHopper.getRouterConfig().setSimplifyResponse(false);
    }

    public RoutingResponse route(RoutingRequest routingRequest) throws RoutingException, RoutingRequestException {
        GHRequest ghRequest = getGHRequest(routingRequest.getWayPoints(), routingRequest.getRoutingProfile());
        if (routingRequest.getCustomModel() != null) {
            ghRequest.setCustomModel(routingRequest.getCustomModel());
        }
        return getRoutingResponse(ghRequest, routingRequest.isSimplifyResponseGeometry());
    }

    private RoutingResponse getRoutingResponse(GHRequest ghRequest, boolean simplify)
            throws RoutingRequestException, RoutingException {
        GHResponse ghResponse = networkGraphHopper.route(ghRequest);
        ensureResponseHasNoErrors(ghResponse);
        ResponsePath responsePath = ghResponse.getBest();
        ensurePathsAreNotEmpty(responsePath);

        RoutingResponseBuilder routingResponseBuilder = createRoutingResponse(responsePath, simplify);
        return setFractionsAndMatchedLinks(routingResponseBuilder, ghRequest);
    }

    private static GHRequest getGHRequest(List<Point> wayPoints, String routingRequest) {
        List<GHPoint> points = getGHPointsFromPoints(wayPoints);
        GHRequest request = new GHRequest(points);
        request.setProfile(routingRequest);
        PMap hints = request.getHints();
        hints.putObject(Routing.CALC_POINTS, true);
        hints.putObject(Routing.INSTRUCTIONS, false);
        hints.putObject(CH.DISABLE, true);
        hints.putObject(Routing.PASS_THROUGH, false);
        return request;
    }

    private RoutingResponse setFractionsAndMatchedLinks(RoutingResponseBuilder routingResponseBuilder,
            GHRequest ghRequest) throws RoutingException {
        List<RoutingLegResponse> routingLegResponse = new ArrayList<>();

        for (Path path : networkGraphHopper.calcPaths(ghRequest)) {
            List<EdgeIteratorState> edges = path.calcEdges();
            if (edges.isEmpty()) {
                throw new RoutingException("Unexpected: path has no edges");
            }
            double startFraction = PathUtil.determineStartLinkFraction(edges.getFirst(),
                    QueryGraphExtractor.extractQueryGraph(path),fractionAndDistanceCalculator);
            double endFraction = PathUtil.determineEndLinkFraction(edges.getLast(),
                    QueryGraphExtractor.extractQueryGraph(path),fractionAndDistanceCalculator);
            List<MatchedEdgeLink> matchedEdgeLinks = PathUtil.determineMatchedLinks(
                    networkGraphHopper.getEncodingManager(),
                    edges);

            routingLegResponse.add(RoutingLegResponse.builder()
                    .matchedLinks(matchedLinkMapper.map(matchedEdgeLinks, startFraction, endFraction))
                    .build());
        }

        routingResponseBuilder.legs(routingLegResponse);

        return routingResponseBuilder.build();
    }

    private RoutingResponseBuilder createRoutingResponse(ResponsePath path, boolean simplify) {
        PointList points = simplify ? PathSimplification.simplify(path, new RamerDouglasPeucker(), false)
                : path.getPoints();
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
            Point waypoint = geometryFactoryWgs84.createPoint(
                    new Coordinate(pointList.getLon(i), pointList.getLat(i)));
            waypoints.add(waypoint);
        }
        return waypoints;
    }
}
