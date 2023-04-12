package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;

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
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
public class GraphHopperRouter implements Router {

    private static final boolean INCLUDE_ELEVATION = false;
    private final NetworkGraphHopper networkGraphHopper;
    private final QueryGraphExtractor queryGraphExtractor;
    private final PathUtil pathUtil;

    @Override
    public RoutingResponse route(RoutingRequest routingRequest) {
        List<GHPoint> ghPoints = getGHPointsFromPoints(routingRequest.getWayPoints());
        GHRequest ghRequest = createGHRequest(ghPoints, routingRequest.getRoutingProfile().getLabel());
        GHResponse routingResponse = networkGraphHopper.route(ghRequest);
        ResponsePath responsePath = routingResponse.getBest();
        ensureResponseHasNoErrors(routingResponse);
        ensurePathsAreNotEmpty(responsePath);
        return createMatchedLinkIds(ghRequest, createRoute(responsePath));
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

    private static void ensurePathsAreNotEmpty(ResponsePath path) {
        if (path.getWaypoints().isEmpty()) {
            throw new IllegalStateException("Calculate resulted in no paths");
        }
    }

    private static void ensureResponseHasNoErrors(GHResponse ghResponse) {
        if (ghResponse.hasErrors()) {
            String errors = ghResponse.getErrors().stream().map(Throwable::getMessage)
                    .collect(Collectors.joining(", "));
            throw new IllegalStateException("Invalid routing request: " + errors);
        }
    }

    private RoutingResponse.RoutingResponseBuilder createRoute(ResponsePath path) {
        return RoutingResponse.builder()
                .geometry(path.getPoints().toLineString(INCLUDE_ELEVATION))
                .weight(Helper.round(path.getRouteWeight(), 1))
                .distance(TimeUnit.MILLISECONDS.toSeconds(path.getTime()))
                .distance(Helper.round(path.getDistance(), 1));


    }

    private RoutingResponse createMatchedLinkIds(GHRequest ghRequest,
            RoutingResponse.RoutingResponseBuilder routeBuilder) {
        int pathIndex = 0;
        List<Integer> matchedLinkIds = new ArrayList<>();
        List<Path> paths = networkGraphHopper.calcPaths(ghRequest);
        for (Path path : paths) {
            List<EdgeIteratorState> edges = path.calcEdges();
            for (int i = 0; i < edges.size(); i++) {
                EdgeIteratorState edge = edges.get(i);
                if (isStartOfRoute(pathIndex, i)) {
                    routeBuilder.startLinkFraction(pathUtil.determineStartLinkFraction(edge,
                            queryGraphExtractor.extractQueryGraph(path)));
                } else if (isEndOfRoute(paths, pathIndex, edges, i)) {
                    routeBuilder.endLinkFraction(pathUtil.determineEndLinkFraction(edge,
                            queryGraphExtractor.extractQueryGraph(path)));
                }
                matchedLinkIds.add(determineLinkId(edge, networkGraphHopper));
            }
            pathIndex++;
        }
        return routeBuilder
                .matchedLinkIds(matchedLinkIds)
                .build();
    }

    private static boolean isEndOfRoute(List<Path> paths, int pathIndex, List<EdgeIteratorState> edges, int i) {
        return pathIndex == paths.size() - 1 && i == edges.size() - 1;
    }

    private static boolean isStartOfRoute(int pathIndex, int i) {
        return pathIndex == 0 && i == 0;
    }

    private int determineLinkId(EdgeIteratorState edge, NetworkGraphHopper graphHopper) {
        return edge.get(graphHopper.getEncodingManager().getIntEncodedValue(ID_NAME));
    }
}
