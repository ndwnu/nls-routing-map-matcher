package nu.ndw.nls.routingmapmatcher.routing;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getNetworkService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.graphhopper.config.Profile;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryRijksdriehoek;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class BikeRouterIT {

    private static final Profile BIKE_PROFILE = new Profile("bike");
    @Autowired
    private RouterFactory routerFactory;
    @Autowired
    private GeometryFactoryRijksdriehoek geometryFactoryRijksdriehoek;

    private Router router;

    @Getter
    public static class BikeLink extends Link {

        @Builder(builderMethodName = "bikeLinkBuilder")
        protected BikeLink(long id, long fromNodeId, long toNodeId, double speedInKilometersPerHour,
                double reverseSpeedInKilometersPerHour, double distanceInMeters, LineString geometry,
                Boolean bicycleAccessible) {
            super(id, fromNodeId, toNodeId, distanceInMeters,
                    geometry, null);
            this.bicycleAccessible = bicycleAccessible;
            this.speed = DirectionalDto.<Double>builder()
                    .forward(speedInKilometersPerHour)
                    .reverse(reverseSpeedInKilometersPerHour)
                    .build();
        }

        @EncodedValue(key = "bicycle_accessible")
        private final Boolean bicycleAccessible;

        @EncodedValue(key = "bike_speed", bits = 1)
        private final DirectionalDto<Double> speed;

    }

    public static class BikeLinkCarMapper extends LinkVehicleMapper<BikeLink> {

        public BikeLinkCarMapper() {
            super("car", BikeLink.class);
        }

        @Override
        public DirectionalDto<Boolean> getAccessibility(BikeLink link) {
            return DirectionalDto.<Boolean>builder().forward(false).reverse(false).build();
        }

        @Override
        public DirectionalDto<Double> getSpeed(BikeLink link) {
            return DirectionalDto.<Double>builder().forward(0.0).reverse(0.0).build();
        }
    }

    public static class BikeLinkBikeMapper extends LinkVehicleMapper<BikeLink> {

        public BikeLinkBikeMapper() {
            super("bike", BikeLink.class);
        }

        @Override
        public DirectionalDto<Boolean> getAccessibility(BikeLink link) {
            return DirectionalDto.<Boolean>builder()
                    .forward(link.getBicycleAccessible())
                    .reverse(link.getBicycleAccessible())
                    .build();
        }

        @Override
        public DirectionalDto<Double> getSpeed(BikeLink link) {
            return link.getSpeed();
        }
    }

    private Point create1DPoint(double x) {
        return geometryFactoryRijksdriehoek.createPoint(new Coordinate(x, 0));
    }

    private BikeLink createBikeLink(long index, long from, long to, Boolean bicycleAccessible) {

        LineString lineString = geometryFactoryRijksdriehoek.createLineString(
                new Coordinate[]{create1DPoint(from).getCoordinate(),
                        create1DPoint(to).getCoordinate()});

        return BikeLink.bikeLinkBuilder()
                .id(index)
                .fromNodeId(from)
                .toNodeId(to)
                .speedInKilometersPerHour(10)
                .reverseSpeedInKilometersPerHour(10)
                .distanceInMeters(Math.abs(from - to))
                .geometry(lineString)
                .bicycleAccessible(bicycleAccessible)
                .build();
    }

    @SneakyThrows
    private void setup(boolean allAccessible) {

        List<LinkVehicleMapper<? extends Link>> vehicles = List.of(new BikeLinkBikeMapper(), new BikeLinkCarMapper());

        List<BikeLink> links = List.of(
                createBikeLink(1, 0, 10, true),
                createBikeLink(2, 10, 20, allAccessible),
                createBikeLink(3, 20, 30, true)
        );

        RoutingNetworkSettings<BikeLink> routingNetworkSettings = RoutingNetworkSettings.builder(BikeLink.class)
                .networkNameAndVersion("test_network")
                .profiles(List.of(BIKE_PROFILE))
                .linkSupplier(links::iterator)
                .build();

        router = routerFactory.createMapMatcher(getNetworkService(vehicles).inMemory(routingNetworkSettings),
                BIKE_PROFILE.getName());
    }

    @Test
    @SneakyThrows
    void route_newWay_allAccessible() {
        setup(true);
        var result = router.route(RoutingRequest.builder()
                .wayPoints(List.of(create1DPoint(1), create1DPoint(28)))
                .build());

        assertEquals(1, result.getLegs().size());
        assertEquals(3, result.getLegs().getFirst().getMatchedLinks().size());
    }

    @Test
    @SneakyThrows
    void route_newWay_linkNotAccessible() {
        setup(false);

        RoutingRequestException routingRequestException = assertThrows(RoutingRequestException.class,
                () -> router.route(RoutingRequest.builder()
                        .wayPoints(List.of(create1DPoint(1), create1DPoint(28)))
                        .build()));

        assertEquals("Invalid routing request: Connection between locations not found",
                routingRequestException.getMessage());
    }
}
