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
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
public class GraphHopperRouter implements Router {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int DECIMAL_PLACES = 3;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final NetworkGraphHopper networkGraphHopper;

    @Override
    public RoutingResponse route(RoutingRequest routingRequest) throws RoutingRequestException, RoutingException {
        List<GHPoint> ghPoints = getGHPointsFromPoints(routingRequest.getWayPoints());
        GHRequest ghRequest = createGHRequest(ghPoints, routingRequest.getRoutingProfile().getLabel());
        networkGraphHopper.getRouterConfig().setSimplifyResponse(routingRequest.isSimplifyResponseGeometry());
        GHResponse routingResponse = networkGraphHopper.route(ghRequest);
        ensureResponseHasNoErrors(routingResponse);
        ResponsePath responsePath = routingResponse.getBest();
        ensurePathsAreNotEmpty(responsePath);
        return createMatchedLinkIds(ghRequest, createRoute(responsePath));
    }

    private RoutingResponse createMatchedLinkIds(GHRequest ghRequest,
            RoutingResponse.RoutingResponseBuilder routeBuilder) {
        List<Path> paths = networkGraphHopper.calcPaths(ghRequest);
        if (!paths.isEmpty()) {
            List<EdgeIteratorState> edges = paths.stream().map(Path::calcEdges).flatMap(Collection::stream).toList();
            routeBuilder
                    .startLinkFraction(PathUtil.determineStartLinkFraction(edges.get(0),
                            QueryGraphExtractor.extractQueryGraph(paths.get(0))))
                    .endLinkFraction(PathUtil.determineEndLinkFraction(edges.get(edges.size() - 1),
                            QueryGraphExtractor.extractQueryGraph(paths.get(paths.size() - 1))))
                    .matchedLinks(PathUtil.determineMatchedLinks(networkGraphHopper.getEncodingManager(), edges));
        }
        return routeBuilder.build();
    }

    private RoutingResponse.RoutingResponseBuilder createRoute(ResponsePath path) {
        return RoutingResponse.builder()
                .geometry(path.getPoints().toLineString(INCLUDE_ELEVATION))
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
