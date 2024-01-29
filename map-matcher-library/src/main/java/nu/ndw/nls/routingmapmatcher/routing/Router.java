package nu.ndw.nls.routingmapmatcher.routing;

import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.WGS84_GEOMETRY_FACTORY;

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
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import nu.ndw.nls.routingmapmatcher.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
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

    public Router(NetworkGraphHopper networkGraphHopper) {
        this.networkGraphHopper = networkGraphHopper;
        // This configuration is global for the routing network and is probably not thread safe.
        // To be able to configure simplification per request, it's safer to disable GraphHopper-internal simplification
        // and perform it in our own response mapping code below.
        this.networkGraphHopper.getRouterConfig().setSimplifyResponse(false);
    }

    public RoutingResponse route(RoutingRequest routingRequest) throws RoutingException, RoutingRequestException {
        GHRequest ghRequest = getGHRequest(routingRequest.getWayPoints(), routingRequest.getRoutingProfile());
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
            GHRequest ghRequest) {
        List<Path> paths = networkGraphHopper.calcPaths(ghRequest);
        if (!paths.isEmpty()) {
            List<EdgeIteratorState> edges = paths.stream().map(Path::calcEdges).flatMap(Collection::stream).toList();
            if (!edges.isEmpty()) {
                routingResponseBuilder
                        .startLinkFraction(PathUtil.determineStartLinkFraction(edges.get(0),
                                QueryGraphExtractor.extractQueryGraph(paths.get(0))))
                        .endLinkFraction(PathUtil.determineEndLinkFraction(edges.get(edges.size() - 1),
                                QueryGraphExtractor.extractQueryGraph(paths.get(paths.size() - 1))))
                        .matchedLinks(PathUtil.determineMatchedLinks(networkGraphHopper.getEncodingManager(), edges));
            }
        }
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
        return points.stream().map(point -> new GHPoint(point.getY(), point.getX()))
                .collect(Collectors.toList());
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

    private static List<Point> mapToSnappedWaypoints(PointList pointList) {
        List<Point> waypoints = new ArrayList<>(pointList.size());
        for (int i = 0; i < pointList.size(); i++) {
            Point waypoint = WGS84_GEOMETRY_FACTORY.createPoint(
                    new Coordinate(pointList.getLon(i), pointList.getLat(i)));
            waypoints.add(waypoint);
        }
        return waypoints;
    }
}
