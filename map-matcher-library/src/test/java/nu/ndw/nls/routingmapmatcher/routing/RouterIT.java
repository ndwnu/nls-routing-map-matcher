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
import org.assertj.core.data.Offset;
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

    private static final MatchedLink MATCHED_LINK_1 = MatchedLink.builder()
            .linkId(7223072)
            .reversed(false)
            .distance(8.357281610810594)
            .startFraction(0.8246035077811711)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_2 = MatchedLink.builder()
            .linkId(7223073)
            .reversed(false)
            .distance(47.60890978696375)
            .startFraction(START_FRACTION_0)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_3 = MatchedLink.builder()
            .linkId(3667130)
            .reversed(false)
            .distance(41.66339422474998)
            .startFraction(START_FRACTION_0)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_4 = MatchedLink.builder()
            .linkId(3667131)
            .reversed(false)
            .distance(41.70294857485283)
            .startFraction(START_FRACTION_0)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_5 = MatchedLink.builder()
            .linkId(3667132)
            .reversed(false)
            .distance(41.663581229545194)
            .startFraction(START_FRACTION_0)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_6 = MatchedLink.builder()
            .linkId(3667133)
            .reversed(false)
            .distance(41.66367473281842)
            .startFraction(START_FRACTION_0)
            .endFraction(END_FRACTION_1)
            .build();
    private static final MatchedLink MATCHED_LINK_7 = MatchedLink.builder()
            .linkId(3666204)
            .reversed(false)
            .distance(20.955710460926817)
            .startFraction(START_FRACTION_0)
            .endFraction(0.5215863361447783)
            .build();

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
    void route_ok_viaPoint() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point via = geometryFactory.createPoint(new Coordinate(5.4295216, 52.1768461));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, via, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        verifySumDistanceOfIndividualRoadSections(result);
        assertViaPointSuccess(result, new Coordinate[]{
                new Coordinate(5.430483, 52.177693),
                new Coordinate(5.429518, 52.176847),
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
        List<MatchedLink> matchedLinks = List.of(
                MATCHED_LINK_1,
                MATCHED_LINK_2,
                MATCHED_LINK_3,
                MATCHED_LINK_4,
                MATCHED_LINK_5,
                MATCHED_LINK_6,
                MATCHED_LINK_7
        );
        assertThat(result.getLegs()).containsExactly(
                RoutingLegResponse.builder()
                        .matchedLinks(matchedLinks)
                        .build());
        assertThat(result.getMatchedLinksGroupedBySameLinkAndDirection()).isEqualTo(matchedLinks);

        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(8.769);
        assertThat(result.getDuration()).isEqualTo(8.768);
        assertThat(result.getDistance()).isEqualTo(243.592);
    }

    private void assertViaPointSuccess(RoutingResponse result, Coordinate[] coordinates) {
        assertThat(result.getLegs()).containsExactly(
                RoutingLegResponse.builder()
                        .matchedLinks(List.of(
                                MATCHED_LINK_1,
                                MATCHED_LINK_2,
                                MATCHED_LINK_3,
                                MATCHED_LINK_4.withDistance(17.244659961414612).withEndFraction(0.4135117671960675)
                        ))
                        .build(),
                RoutingLegResponse.builder()
                        .matchedLinks(List.of(
                                MATCHED_LINK_4.withDistance(24.458288612514636).withStartFraction(0.4135117671960675),
                                MATCHED_LINK_5,
                                MATCHED_LINK_6,
                                MATCHED_LINK_7
                        ))
                        .build());
        assertThat(result.getMatchedLinksGroupedBySameLinkAndDirection()).isEqualTo(List.of(
                MATCHED_LINK_1,
                MATCHED_LINK_2,
                MATCHED_LINK_3,
                // Tiny rounding difference compared to original
                MATCHED_LINK_4.withDistance(41.70294857392925),
                MATCHED_LINK_5,
                MATCHED_LINK_6,
                MATCHED_LINK_7
        ));

        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(8.768);
        assertThat(result.getDuration()).isEqualTo(8.767);
        assertThat(result.getDistance()).isEqualTo(243.558);
    }

    private void assertSnapToNodesSuccess(RoutingResponse result, Coordinate[] coordinates) {
        List<MatchedLink> matchedLinks = List.of(
                MATCHED_LINK_2,
                MATCHED_LINK_3,
                MATCHED_LINK_4,
                MATCHED_LINK_5,
                MATCHED_LINK_6,
                MATCHED_LINK_7.withDistance(40.17687774533663).withEndFraction(END_FRACTION_1)
        );
        assertThat(result.getLegs()).containsExactly(
                RoutingLegResponse.builder()
                        .matchedLinks(matchedLinks)
                        .build());
        assertThat(result.getMatchedLinksGroupedBySameLinkAndDirection()).isEqualTo(matchedLinks);

        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(9.164);
        assertThat(result.getDuration()).isEqualTo(9.163);
        assertThat(result.getDistance()).isEqualTo(254.546);
    }

    private static void verifySumDistanceOfIndividualRoadSections(RoutingResponse response) {
        double matchedLinksDistance = response.getLegs().stream()
                .flatMap(l -> l.getMatchedLinks().stream())
                .mapToDouble(MatchedLink::getDistance)
                .sum();
        assertThat(matchedLinksDistance)
                .isCloseTo(response.getDistance(), Offset.offset(0.1));
        double matchedLinksGroupedDistance = response.getMatchedLinksGroupedBySameLinkAndDirection().stream()
                .mapToDouble(MatchedLink::getDistance)
                .sum();
        assertThat(matchedLinksGroupedDistance)
                .isCloseTo(matchedLinksDistance, Offset.offset(0.0005));
    }
}
