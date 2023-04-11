package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.ResponsePath;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.routing.querygraph.QueryGraph;
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
import nu.ndw.nls.routingmapmatcher.domain.model.routing.Route;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.Waypoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.locationtech.jts.geom.LineString;

@RequiredArgsConstructor
public class GraphHopperRouter implements Router {

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
        return RoutingResponse.builder()
                .route(createMatchedLinkIds(ghRequest, createRoute(responsePath)))
                .waypoints(ghPoints.stream()
                        .map(ghPoint -> Waypoint
                                .builder()
                                .latitude(ghPoint.getLat())
                                .longitude(ghPoint.getLon())
                                .build()).toList())
                .build();
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


    private static List<GHPoint> getGHPointsFromPoints(List<List<Double>> points) {
        return points.stream().map(coordinates -> new GHPoint(coordinates.get(1), coordinates.get(0)))
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

    private Route.RouteBuilder createRoute(ResponsePath path) {

        LineString lineString = path.getPoints().toLineString(false);
        double weight = Helper.round(path.getRouteWeight(), 1);
        long duration = TimeUnit.MILLISECONDS.toSeconds(path.getTime());
        double distance = Helper.round(path.getDistance(), 1);
        Route.RouteBuilder routeBuilder = Route.builder();
        return routeBuilder
                .geometry(lineString)
                .weight(weight)
                .distance(duration)
                .distance(distance);


    }

    private Route createMatchedLinkIds(GHRequest ghRequest, Route.RouteBuilder routeBuilder) {
        List<Path> paths = networkGraphHopper.calcPaths(ghRequest);
        int pathIndex = 0;
        List<Integer> matchedLinkIds = new ArrayList<>();
        for (Path path : paths) {
            QueryGraph queryGraph = queryGraphExtractor.extractQueryGraph(path);
            List<EdgeIteratorState> edges = path.calcEdges();
            for (int i = 0; i < edges.size(); i++) {
                EdgeIteratorState edge = edges.get(i);
                if (pathIndex == 0 && i == 0) {
                    routeBuilder.startFraction(pathUtil.determineStartLinkFraction(edges.get(i),
                            queryGraph));
                } else if (pathIndex == paths.size() - 1 && i == edges.size() - 1) {
                    routeBuilder.endFraction(pathUtil.determineEndLinkFraction(edges.get(i),
                            queryGraph));
                }
                matchedLinkIds.add(determineLinkId(edge, networkGraphHopper));
            }
            pathIndex++;
        }
        return routeBuilder
                .matchedLinkIds(matchedLinkIds)
                .build();
    }

    private int determineLinkId(EdgeIteratorState edge, NetworkGraphHopper graphHopper) {
        return edge.get(graphHopper.getEncodingManager().getIntEncodedValue(ID_NAME));
    }
}
