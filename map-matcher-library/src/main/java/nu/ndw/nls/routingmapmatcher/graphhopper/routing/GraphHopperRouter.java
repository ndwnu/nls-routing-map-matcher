package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WGS84_GEOMETRY_FACTORY;

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
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse.RoutingResponseBuilder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

public class GraphHopperRouter implements Router {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int DECIMAL_PLACES = 3;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final NetworkGraphHopper networkGraphHopper;

    public GraphHopperRouter(NetworkGraphHopper networkGraphHopper) {
        this.networkGraphHopper = networkGraphHopper;
        // This configuration is global for the routing network and is probably not thread safe.
        // To be able to configure simplification per request, it's safer to disable GraphHopper-internal simplification
        // and perform it in our own response mapping code below.
        this.networkGraphHopper.getRouterConfig().setSimplifyResponse(false);
    }

    @Override
    public RoutingResponse route(RoutingRequest routingRequest) throws RoutingRequestException, RoutingException {
        List<GHPoint> ghPoints = getGHPointsFromPoints(routingRequest.getWayPoints());
        GHRequest ghRequest = createGHRequest(ghPoints, routingRequest.getRoutingProfile().getLabel());
        GHResponse ghResponse = networkGraphHopper.route(ghRequest);
        ensureResponseHasNoErrors(ghResponse);
        ResponsePath responsePath = ghResponse.getBest();
        ensurePathsAreNotEmpty(responsePath);

        boolean simplify = routingRequest.isSimplifyResponseGeometry();
        RoutingResponseBuilder routingResponseBuilder = createRoutingResponse(responsePath, simplify);
        return setFractionsAndMatchedLinks(routingResponseBuilder, ghRequest);
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

    private static GHRequest createGHRequest(List<GHPoint> points, String profile) {
        GHRequest request = new GHRequest(points);
        request.setProfile(profile);
        PMap hints = request.getHints();
        hints.putObject(Routing.CALC_POINTS, true);
        hints.putObject(Routing.INSTRUCTIONS, false);
        hints.putObject(CH.DISABLE, true);
        hints.putObject(Routing.PASS_THROUGH, false);
        return request;
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
