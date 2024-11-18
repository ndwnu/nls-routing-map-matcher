package nu.ndw.nls.routingmapmatcher.routing;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getNetworkService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.graphhopper.config.Profile;
import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingLegResponse;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import org.assertj.core.data.Percentage;
import org.geotools.geometry.jts.WKTReader2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class RoutingFractionsIT {

    private static final String VEHICLE = "car";
    private static final String PROFILE_NAME = "car";
    private static final Profile PROFILE = new Profile(VEHICLE);
    private static final String NETWORK_NAME = "test_network";
    @Autowired
    private RouterFactory routerFactory;
    @Autowired
    private GeometryFactoryWgs84 geometryFactoryWgs84;
    private Router router;


    // Our sample data contains one way driving directions, but we need to simulate a U-turn to get fractions.
    // This vehicle mapper ignores the restrictions, so we can still simulate it
    public static class AlwaysAccessibleAndHasSpeedVehicleMapper extends
            LinkVehicleMapper<TestNetworkProvider.TestLink> {

        private static final double ALWAYS_SPEED = 10.0;

        public AlwaysAccessibleAndHasSpeedVehicleMapper() {
            super(VEHICLE, TestNetworkProvider.TestLink.class);
        }

        @Override
        public DirectionalDto<Boolean> getAccessibility(TestNetworkProvider.TestLink link) {
            return DirectionalDto.<Boolean>builder()
                    .forward(true)
                    .reverse(true).build();
        }

        @Override
        public DirectionalDto<Double> getSpeed(TestNetworkProvider.TestLink link) {
            return DirectionalDto.<Double>builder()
                    .forward(ALWAYS_SPEED)
                    .reverse(ALWAYS_SPEED)
                    .build();
        }
    }


    @BeforeEach
    @SneakyThrows
    public void setup() {

        List<TestNetworkProvider.TestLink> testLinks = TestNetworkProvider.getTestLinks("/test-data/links.json");

        RoutingNetworkSettings<TestNetworkProvider.TestLink> routingNetworkSettings =
                RoutingNetworkSettings.builder(TestNetworkProvider.TestLink.class)
                        .networkNameAndVersion(NETWORK_NAME)
                        .profiles(List.of(PROFILE))
                        .linkSupplier(testLinks::iterator)
                        .build();

        router = routerFactory.createMapMatcher(
                getNetworkService(List.of(new AlwaysAccessibleAndHasSpeedVehicleMapper()))
                        .inMemory(routingNetworkSettings), PROFILE.getName());

    }

    @Test
    @SneakyThrows
    void route_ok_fractionsInDirectionOfTravel() {
        Point pointA = createPoint(5.42573374, 52.17980818);
        Point pointB = createPoint(5.42585928, 52.18010440);

        // Request a route over a single link
        var resultA = router.route(RoutingRequest.builder()
                .routingProfile(PROFILE_NAME)
                .wayPoints(List.of(pointA, pointB))
                .build());
        verifySumDistanceOfIndividualRoadSections(resultA);
        assertEquals(1, resultA.getLegs().size());
        RoutingLegResponse routingLegResponseA = resultA.getLegs().getFirst();
        assertEquals(1, routingLegResponseA.getMatchedLinks().size());
        MatchedLink matchedLinkA = routingLegResponseA.getMatchedLinks().getFirst();
        assertEquals(6405185, matchedLinkA.getLinkId());
        assertTrue(matchedLinkA.isReversed());
        assertEquals(0.07063478982795716, matchedLinkA.getStartFraction());
        assertEquals(0.7828837347130593, matchedLinkA.getEndFraction());

        // Now request the same route, but in reverse direction
        var resultB = router.route(RoutingRequest.builder()
                .routingProfile(PROFILE_NAME)
                .wayPoints(List.of(pointB, pointA))
                .build());
        verifySumDistanceOfIndividualRoadSections(resultB);
        assertEquals(1, resultB.getLegs().size());
        RoutingLegResponse routingLegResponseB = resultB.getLegs().getFirst();
        List<MatchedLink> matchedLinks = routingLegResponseB.getMatchedLinks();
        assertEquals(1, matchedLinks.size());
        MatchedLink matchedLinkB = matchedLinks.getFirst();
        assertEquals(6405185, matchedLinkB.getLinkId());
        assertFalse(matchedLinkB.isReversed());
        assertEquals(0.21711626527479014, matchedLinkB.getStartFraction());
        assertEquals(0.9293652101628306, matchedLinkB.getEndFraction());

        assertEquals(1.0, matchedLinkA.getStartFraction() + matchedLinkB.getEndFraction(), 0.0000001,
                "Fractions are in the direction of driving, therefore the sum of the fraction and the fraction of the "
                + "same point traveling in the reverse direction should always end up as 1.0");
        assertEquals(1.0, matchedLinkA.getEndFraction() + matchedLinkB.getStartFraction(), 0.0000001,
                "Fractions are in the direction of driving, therefore the sum of the fraction and the fraction of the "
                + "same point traveling in the in reverse direction should always end up as 1.0");
    }

    /**
     * This test is using a part of north of the the 'Amersfoort knoopunt' that looks like this: --┴-- First point is in
     * the west, then turning north and last point is east
     */
    @Test
    @SneakyThrows
    void route_ok_uTurnResultsInLegsWithFractions() {
        var result = router.route(RoutingRequest.builder()
                .routingProfile(PROFILE_NAME)
                .wayPoints(List.of(createPoint(5.42511239, 52.17985105),
                        createPoint(5.42576075, 52.17986470),
                        createPoint(5.42639323, 52.17976530)))
                .build());
        verifySumDistanceOfIndividualRoadSections(result);
        assertThat(result).isEqualTo(RoutingResponse.builder()
                .geometry(createLineString())
                .snappedWaypoints(List.of(createPoint(5.425122870485016, 52.17986902304874),
                        createPoint(5.425755082159361, 52.17986556367133),
                        createPoint(5.426393415313249, 52.179772178137206)))
                .distance(107.739)
                .weight(38.786)
                .duration(38.787)
                .legs(List.of(RoutingLegResponse.builder()
                                .matchedLinks(List.of(
                                        MatchedLink.builder()
                                                .linkId(6369283)
                                                .reversed(false)
                                                .distance(42.10921482022403)
                                                .startFraction(0.11164377136022784)
                                                .endFraction(1.0)
                                                .geometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.425122870485016, 52.17986902304874),
                                                                new Coordinate(5.425237, 52.179844),
                                                                new Coordinate(5.42572, 52.179779)
                                                        }))
                                                .originalGeometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.42505, 52.179885),
                                                                new Coordinate(5.425237, 52.179844),
                                                                new Coordinate(5.42572, 52.179779)
                                                        }))
                                                .build(),
                                        MatchedLink.builder()
                                                .linkId(6405185)
                                                .reversed(true)
                                                .distance(9.926436244977584)
                                                .startFraction(0.0)
                                                .endFraction(0.2075867812415673)
                                                .geometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.42572, 52.179779),
                                                                new Coordinate(5.425755082159361, 52.17986556367133)
                                                        }))
                                                .originalGeometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.42572, 52.179779),
                                                                new Coordinate(5.425889, 52.180196)
                                                        }))
                                                .build())
                                ).build(),
                        RoutingLegResponse.builder()
                                .matchedLinks(List.of(
                                        MatchedLink.builder()
                                                .linkId(6405185)
                                                .reversed(false)
                                                .distance(9.926436244977584)
                                                .startFraction(0.7924132187612766)
                                                .endFraction(1.0)
                                                .geometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.425755082159361, 52.17986556367133),
                                                                new Coordinate(5.42572, 52.179779)
                                                        }))
                                                .originalGeometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.425889, 52.180196),
                                                                new Coordinate(5.42572, 52.179779)
                                                        }))
                                                .build(),
                                        MatchedLink.builder()
                                                .linkId(6405218)
                                                .reversed(false)
                                                .distance(46.06958804725149)
                                                .startFraction(0.0)
                                                .endFraction(0.9745518261996916)
                                                .geometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.42572, 52.179779),
                                                                new Coordinate(5.426393415313249, 52.179772178137206)
                                                        }))
                                                .originalGeometry(geometryFactoryWgs84.createLineString(
                                                        new Coordinate[]{
                                                                new Coordinate(5.42572, 52.179779),
                                                                new Coordinate(5.426411, 52.179772)
                                                        }))
                                                .build())
                                ).build()
                )).build()

        );

    }

    private static void verifySumDistanceOfIndividualRoadSections(RoutingResponse response) {
        assertThat((Double) response.getMatchedLinks()
                .stream()
                .map(MatchedLink::getDistance)
                .mapToDouble(Double::doubleValue)
                .sum())
                .isCloseTo(response.getDistance(), Percentage.withPercentage(0.3));
    }
    @SneakyThrows
    private LineString createLineString() {
        WKTReader wktReader = new WKTReader2();
        return (LineString) wktReader.read(
                "LINESTRING (5.425123 52.179869, 5.42572 52.179779, 5.425755 52.179866, 5.42572 52.179779, 5.426393 52.179772)");
    }

    private Point createPoint(double x, double y) {
        return geometryFactoryWgs84.createPoint(
                new Coordinate(x, y));
    }
}
