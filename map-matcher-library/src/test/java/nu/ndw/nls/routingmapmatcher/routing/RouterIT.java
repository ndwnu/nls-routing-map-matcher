package nu.ndw.nls.routingmapmatcher.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class RouterIT {

    private Router router;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        router = new Router(TestNetworkProvider.getTestNetworkFromFile("/test-data/links.json"));
        geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
    }

    @SneakyThrows
    @Test
    void route_ok() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile("car_fastest")
                .wayPoints(wayPoints)
                .build());
        assertSuccess(result, new Coordinate[]{new Coordinate(5.430483, 52.177693),
                new Coordinate(5.428436, 52.175901)});
    }

    @SneakyThrows
    @Test
    void route_ok_noSimplify() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile("car_fastest")
                .wayPoints(wayPoints)
                .simplifyResponseGeometry(false)
                .build());
        assertSuccess(result, new Coordinate[]{new Coordinate(5.430483, 52.177693), new Coordinate(5.430413, 52.177631),
                new Coordinate(5.430015, 52.17728), new Coordinate(5.429664, 52.176974),
                new Coordinate(5.429312, 52.176668), new Coordinate(5.428961, 52.176362),
                new Coordinate(5.42861, 52.176056), new Coordinate(5.428436, 52.175901)});
    }

    private void assertSuccess(RoutingResponse result, Coordinate[] coordinates) {
        assertThat(result.getStartLinkFraction()).isEqualTo(0.8246035077811711);
        assertThat(result.getEndLinkFraction()).isEqualTo(0.5215863361447783);
        assertThat(result.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(7223072).reversed(false).build(),
                MatchedLink.builder().linkId(7223073).reversed(false).build(),
                MatchedLink.builder().linkId(3667130).reversed(false).build(),
                MatchedLink.builder().linkId(3667131).reversed(false).build(),
                MatchedLink.builder().linkId(3667132).reversed(false).build(),
                MatchedLink.builder().linkId(3667133).reversed(false).build(),
                MatchedLink.builder().linkId(3666204).reversed(false).build());
        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(8.769);
        assertThat(result.getDuration()).isEqualTo(8.768);
        assertThat(result.getDistance()).isEqualTo(243.592);
    }
}
