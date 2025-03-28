package nu.ndw.nls.routingmapmatcher.routing;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;

import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class RouterIT {

    private static final double START_FRACTION_0 = 0.0;
    private static final double END_FRACTION_1 = 1.0;

    @Autowired
    private RouterFactory routerFactory;

    @Autowired
    private GeometryFactoryWgs84 geometryFactory;

    private Router router;

    @SneakyThrows
    private void setupNetwork() {
        router = routerFactory.createMapMatcher(TestNetworkProvider.getTestNetworkFromFile("/test-data/network.geojson"), CAR);
    }

    @SneakyThrows
    @Test
    void route_ok() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        verifySumDistanceOfIndividualRoadSections(result);
        assertSuccess(result, new Coordinate[]{
                new Coordinate(5.430483, 52.177693),
                new Coordinate(5.428436, 52.175901)
        });
    }

    @SneakyThrows
    @Test
    void route_ok_snapToNodes() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .snapToNodes(true)
                .build());
        verifySumDistanceOfIndividualRoadSections(result);
        assertSnapToNodesSuccess(result, new Coordinate[]{
                new Coordinate(5.430413, 52.177631),
                new Coordinate(5.428276, 52.175759)
        });
    }

    @SneakyThrows
    @Test
    void route_outOfBounds_snapToNodes() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point outOfBounds = geometryFactory.createPoint(new Coordinate(5.430496, 42.0));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, outOfBounds, end);
        assertThatException()
                .isThrownBy(
                        () -> router.route(RoutingRequest.builder()
                                .routingProfile(CAR)
                                .wayPoints(wayPoints)
                                .snapToNodes(true)
                                .build())
                )
                .isInstanceOf(RoutingRequestException.class)
                .withMessage("Invalid routing request: Point is out of bounds: POINT (5.430496 42), "
                             + "the bounds are: 4.9467900079047,5.433661,52.172107,52.63028869479728");
    }

    @SneakyThrows
    @Test
    void route_cannotSnap_snapToNodes() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point cannotSnap = geometryFactory.createPoint(new Coordinate(5.430496, 52.318371));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, cannotSnap, end);
        assertThatException()
                .isThrownBy(
                        () -> router.route(RoutingRequest.builder()
                                .routingProfile(CAR)
                                .wayPoints(wayPoints)
                                .snapToNodes(true)
                                .build())
                )
                .isInstanceOf(RoutingRequestException.class)
                .withMessage("Invalid routing request: Cannot snap point 52.318371,5.430496 to node");
    }

    @SneakyThrows
    @Test
    void route_sameNode_snapToNodes() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point sameAsStart = geometryFactory.createPoint(new Coordinate(5.430500, 52.177700));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, sameAsStart, end);
        assertThatException()
                .isThrownBy(
                        () -> router.route(RoutingRequest.builder()
                                .routingProfile(CAR)
                                .wayPoints(wayPoints)
                                .snapToNodes(true)
                                .build())
                )
                .isInstanceOf(RoutingRequestException.class)
                .withMessage("Invalid routing request: Points are snapped to the same node");
    }

    @SneakyThrows
    @Test
    void route_ok_noSimplify() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        RoutingResponse result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .simplifyResponseGeometry(false)
                .build());
        verifySumDistanceOfIndividualRoadSections(result);
        assertSuccess(result, new Coordinate[]{
                new Coordinate(5.430483, 52.177693),
                new Coordinate(5.430413, 52.177631),
                new Coordinate(5.430015, 52.17728),
                new Coordinate(5.429664, 52.176974),
                new Coordinate(5.429312, 52.176668),
                new Coordinate(5.428961, 52.176362),
                new Coordinate(5.42861, 52.176056),
                new Coordinate(5.428436, 52.175901)
        });
    }

    private void assertSuccess(RoutingResponse result, Coordinate[] coordinates) {
        assertThat(result.getLegs()).containsExactly(
                RoutingLegResponse.builder()
                        .matchedLinks(List.of(
                                MatchedLink.builder()
                                        .linkId(7223072)
                                        .reversed(false)
                                        .distance(8.357281610810594)
                                        .startFraction(0.8246035077811711)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(7223073)
                                        .reversed(false)
                                        .distance(47.60890978696375)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667130)
                                        .reversed(false)
                                        .distance(41.66339422474998)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667131)
                                        .reversed(false)
                                        .distance(41.70294857485283)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667132)
                                        .reversed(false)
                                        .distance(41.663581229545194)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667133)
                                        .reversed(false)
                                        .distance(41.66367473281842)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3666204)
                                        .reversed(false)
                                        .distance(20.955710460926817)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(0.5215863361447783)
                                        .build()
                        ))
                        .build());

        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(8.769);
        assertThat(result.getDuration()).isEqualTo(8.768);
        assertThat(result.getDistance()).isEqualTo(243.592);
    }

    private void assertSnapToNodesSuccess(RoutingResponse result, Coordinate[] coordinates) {
        assertThat(result.getLegs()).containsExactly(
                RoutingLegResponse.builder()
                        .matchedLinks(List.of(
                                MatchedLink.builder()
                                        .linkId(7223073)
                                        .reversed(false)
                                        .distance(47.60890978696375)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667130)
                                        .reversed(false)
                                        .distance(41.66339422474998)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667131)
                                        .reversed(false)
                                        .distance(41.70294857485283)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667132)
                                        .reversed(false)
                                        .distance(41.663581229545194)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3667133)
                                        .reversed(false)
                                        .distance(41.66367473281842)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build(),
                                MatchedLink.builder()
                                        .linkId(3666204)
                                        .reversed(false)
                                        .distance(40.17687774533663)
                                        .startFraction(START_FRACTION_0)
                                        .endFraction(END_FRACTION_1)
                                        .build()
                        ))
                        .build());

        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(9.164);
        assertThat(result.getDuration()).isEqualTo(9.163);
        assertThat(result.getDistance()).isEqualTo(254.546);
    }

    private static void verifySumDistanceOfIndividualRoadSections(RoutingResponse response) {
        assertThat((Double) response.getMatchedLinks()
                .stream()
                .map(MatchedLink::getDistance)
                .mapToDouble(Double::doubleValue)
                .sum())
                .isCloseTo(response.getDistance(), Percentage.withPercentage(0.1));
    }
}
