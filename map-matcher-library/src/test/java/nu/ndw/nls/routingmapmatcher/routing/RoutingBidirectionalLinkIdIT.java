
package nu.ndw.nls.routingmapmatcher.routing;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestNetwork;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class RoutingBidirectionalLinkIdIT {

    @Autowired
    private RouterFactory routerFactory;

    @Autowired
    private GeometryFactoryWgs84 geometryFactoryWgs84;

    private Router router;

    @SneakyThrows
    void setupReversedGeometry() {
        router = routerFactory.createMapMatcher(getTestNetwork(createLinksNoCorrectAlignment()),
                CAR);
    }

    @SneakyThrows
    void setupNormalisedNetwork() {
        router = routerFactory.createMapMatcher(getTestNetwork(createLinksCorrectAlignment()),
                CAR);
    }

    /**
     * returns 4 unidirectional links with incorrect geometric alignment
     */
    private List<TestLink> createLinksNoCorrectAlignment() {
        return List.of(
                createLineLink(0, 10, 20, 50, 0, 1, 0, 2),
                createLineLink(1, 20, 10, 50, 0, 2, 0, 1),
                createLineLink(2, 20, 30, 50, 0, 2, 0, 3),
                createLineLink(3, 30, 20, 50, 0, 3, 0, 2)
        );
    }

    /**
     * returns 2 bidirectional links with correct geometric alignment and directional link id
     */
    private List<TestLink> createLinksCorrectAlignment() {
        return List.of(
                createBiDirectionalLineLink(1, 2, 10, 20, 50, 50, 0, 1, 0, 2),
                createBiDirectionalLineLink(3, 4, 20, 30, 50, 50, 0, 2, 0, 3)
        );
    }

    private TestLink createLineLink(long id, long fromNodeId, long toNodeId, double speed, double... coordinates) {
        return TestLink.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speed)
                .reverseSpeedInKilometersPerHour(0)
                .distanceInMeters(100)
                .geometry(createLineStringWktReader(coordinates))
                .build();
    }

    private TestLink createBiDirectionalLineLink(long id, long reverseLinkId, long fromNodeId, long toNodeId,
            double speed, double reverseSpeed, double... coordinates) {
        return TestLink.builder()
                .id(id)
                .linkIdReversed(reverseLinkId)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speed)
                .reverseSpeedInKilometersPerHour(reverseSpeed)
                .distanceInMeters(100)
                .geometry(createLineStringWktReader(coordinates))
                .build();
    }

    @SneakyThrows
    private LineString createLineStringWktReader(double... coordinates) {
        if (coordinates == null || coordinates.length % 2 != 0) {
            throw new IllegalStateException("Must have coordinates and must come in pairs of two (x, y)");
        }

        StringBuilder lineStringSb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coordinates.length; i += 2) {
            if (i > 0) {
                lineStringSb.append(", ");
            }
            lineStringSb.append(coordinates[i]);
            lineStringSb.append(" ");
            lineStringSb.append(coordinates[i + 1]);
        }

        lineStringSb.append(")");

        log.debug("Loading line string: {}", lineStringSb);
        WKTReader wktReader = new WKTReader(geometryFactoryWgs84);
        return (LineString) wktReader.read(lineStringSb.toString());
    }

    @SneakyThrows
    @Test
    void route_ok_reverse() {
        setupReversedGeometry();
        Point start = geometryFactoryWgs84.createPoint(new Coordinate(0, 3));
        Point end = geometryFactoryWgs84.createPoint(new Coordinate(0, 1));
        List<Point> wayPoints = List.of(start, end);
        RoutingResponse response = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        assertThat(response.getMatchedLinks())
                .hasSize(2)
                .satisfiesExactly(
                        first -> {
                            assertThat(first.getLinkId()).isEqualTo(3);
                            assertThat(first.isReversed()).isFalse();
                        },
                        last -> {
                            assertThat(last.getLinkId()).isEqualTo(1);
                            assertThat(last.isReversed()).isFalse();
                        }
                );
    }

    @SneakyThrows
    @Test
    void route_ok_forward() {
        setupReversedGeometry();
        Point start = geometryFactoryWgs84.createPoint(new Coordinate(0, 1));
        Point end = geometryFactoryWgs84.createPoint(new Coordinate(0, 3));
        List<Point> wayPoints = List.of(start, end);
        RoutingResponse response = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        assertThat(response.getMatchedLinks())
                .hasSize(2)
                .satisfiesExactly(
                        first -> {
                            assertThat(first.getLinkId()).isEqualTo(0);
                            assertThat(first.isReversed()).isFalse();
                        },
                        last -> {
                            assertThat(last.getLinkId()).isEqualTo(2);
                            assertThat(last.isReversed()).isFalse();
                        }
                );
    }

    @SneakyThrows
    @Test
    void route_ok_normalised_reversed() {
        setupNormalisedNetwork();
        Point start = geometryFactoryWgs84.createPoint(new Coordinate(0, 3));
        Point end = geometryFactoryWgs84.createPoint(new Coordinate(0, 1));
        List<Point> wayPoints = List.of(start, end);
        RoutingResponse result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        assertThat(result.getMatchedLinks())
                .hasSize(2)
                .satisfiesExactly(
                        first -> {
                            assertThat(first.getLinkId()).isEqualTo(4);
                            assertThat(first.isReversed()).isFalse();
                        },
                        last -> {
                            assertThat(last.getLinkId()).isEqualTo(2);
                            assertThat(last.isReversed()).isFalse();
                        }
                );
    }

    @SneakyThrows
    @Test
    void route_ok_normalised_forward() {
        setupNormalisedNetwork();
        Point start = geometryFactoryWgs84.createPoint(new Coordinate(0, 1));
        Point end = geometryFactoryWgs84.createPoint(new Coordinate(0, 3));
        List<Point> wayPoints = List.of(start, end);
        RoutingResponse result = router.route(RoutingRequest.builder()
                .routingProfile(CAR)
                .wayPoints(wayPoints)
                .build());
        assertThat(result.getMatchedLinks())
                .hasSize(2)
                .satisfiesExactly(
                        first -> {
                            assertThat(first.getLinkId()).isEqualTo(1);
                            assertThat(first.isReversed()).isFalse();
                        },
                        last -> {
                            assertThat(last.getLinkId()).isEqualTo(3);
                            assertThat(last.isReversed()).isFalse();
                        }
                );
    }
}
