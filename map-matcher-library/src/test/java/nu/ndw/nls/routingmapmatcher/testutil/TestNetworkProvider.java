package nu.ndw.nls.routingmapmatcher.testutil;

import com.graphhopper.config.Profile;
import com.graphhopper.util.TurnCostsConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.GraphHopperNetworkService;
import nu.ndw.nls.routingmapmatcher.network.LinkVehicleMapperProvider;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.AnnotationMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.DirectionalFieldGenericTypeArgumentMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.EncodedValuesMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedMapperFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.EncodedValueFactoryRegistry;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedBooleanValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedDoubleValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedIntegerValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedLongValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedStringValueFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedBooleanMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedDoubleMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedIntegerMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedLongMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedStringMapperFactory;
import nu.ndw.nls.routingmapmatcher.network.mappers.CustomModelMapper;
import nu.ndw.nls.routingmapmatcher.network.mappers.ProfileAccessAndSpeedAttributesMapper;
import nu.ndw.nls.routingmapmatcher.network.mappers.SpeedAndAccessAttributeMapper;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import org.apache.commons.io.IOUtils;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.LineString;

public final class TestNetworkProvider {

    public static final String HGV_ACCESSIBLE_KEY = "hgv_accessible";
    public static final String CAR = "car";
    public static final String CAR_NO_U_TURNS = "car_no_u_turns";
    public static List<Profile> TEST_PROFILES = List.of(
            new Profile(CAR),
            new Profile(CAR_NO_U_TURNS)
                    .setTurnCostsConfig(new TurnCostsConfig(List.of("motor_vehicle"))
                            .setUTurnCosts(TurnCostsConfig.INFINITE_U_TURN_COSTS))
    );

    private TestNetworkProvider() {
        // Util class
    }

    public static GraphHopperNetworkService getNetworkService(
            List<LinkVehicleMapper<? extends Link>> vehicleProviders) {
        return new GraphHopperNetworkService(
                getLinkVehicleProvider(vehicleProviders), getEncoderFactories(), getEncodedValuesMapper(),
                getParserFactories(), getCustomModelMapper(), getProfileAccessAndSpeedAttributesMapper());
    }

    @NotNull
    private static ProfileAccessAndSpeedAttributesMapper getProfileAccessAndSpeedAttributesMapper() {
        return new ProfileAccessAndSpeedAttributesMapper(
                new SpeedAndAccessAttributeMapper());
    }

    @NotNull
    private static CustomModelMapper getCustomModelMapper() {
        return new CustomModelMapper(
                new SpeedAndAccessAttributeMapper());
    }

    public static final GraphHopperNetworkService NETWORK_SERVICE = getNetworkService(
            List.of(new TestLinkCarMapper(CAR),
                    new TestLinkCarMapper(CAR_NO_U_TURNS)));

    public static List<TestLink> getTestLinks(String path) throws IOException {
        String linksJson;
        try (InputStream resourceAsStream = TestNetworkProvider.class.getResourceAsStream(path)) {
            linksJson = IOUtils.toString(Objects.requireNonNull(resourceAsStream), StandardCharsets.UTF_8);
        }
        List<TestLink> links = new ArrayList<>();
        try (GeoJSONReader geoJSONReader = new GeoJSONReader(linksJson)) {
            SimpleFeatureIterator it = geoJSONReader.getIterator();
            while (it.hasNext()) {
                links.add(LinkDeserializer.deserialize(it.next()));
            }
        }
        return links;
    }

    public static NetworkGraphHopper getTestNetwork(List<TestLink> links) {
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion("test_network")
                .profiles(TEST_PROFILES)
                .linkSupplier(links::iterator)
                .build();

        return NETWORK_SERVICE.inMemory(routingNetworkSettings);
    }

    public static NetworkGraphHopper getTestNetworkFromFile(String path) throws IOException {
        List<TestLink> links = getTestLinks(path);
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion("test_network")
                .profiles(TEST_PROFILES)
                .linkSupplier(links::iterator)
                .build();

        return NETWORK_SERVICE.inMemory(routingNetworkSettings);
    }

    private static EncodedValuesMapper getEncodedValuesMapper() {
        return new EncodedValuesMapper(new AnnotationMapper(), new DirectionalFieldGenericTypeArgumentMapper());
    }

    private static EncodedMapperFactoryRegistry getParserFactories() {
        return new EncodedMapperFactoryRegistry(List.of(
                new EncodedBooleanMapperFactory(),
                new EncodedDoubleMapperFactory(),
                new EncodedIntegerMapperFactory(),
                new EncodedStringMapperFactory(),
                new EncodedLongMapperFactory()));
    }

    private static EncodedValueFactoryRegistry getEncoderFactories() {
        return new EncodedValueFactoryRegistry(List.of(
                new EncodedBooleanValueFactory(),
                new EncodedDoubleValueFactory(),
                new EncodedIntegerValueFactory(),
                new EncodedLongValueFactory(),
                new EncodedStringValueFactory()
        ));
    }

    private static LinkVehicleMapperProvider getLinkVehicleProvider(List<LinkVehicleMapper<? extends Link>> vehicles) {
        return new LinkVehicleMapperProvider(vehicles);
    }

    @Getter
    public static class TestLink extends Link {

        private final double speedInKilometersPerHour;
        private final double reverseSpeedInKilometersPerHour;

        @EncodedValue(key = HGV_ACCESSIBLE_KEY, bits = 1)
        private final Boolean hgvAccessible;

        @Builder
        protected TestLink(long id, long fromNodeId, long toNodeId, double speedInKilometersPerHour,
                double reverseSpeedInKilometersPerHour, Boolean hgvAccessible, double distanceInMeters,
                LineString geometry, Long linkIdReversed) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry, linkIdReversed);
            this.speedInKilometersPerHour = speedInKilometersPerHour;
            this.reverseSpeedInKilometersPerHour = reverseSpeedInKilometersPerHour;
            this.hgvAccessible = Optional.ofNullable(hgvAccessible).orElse(true);
        }
    }

    public static class TestLinkCarMapper extends LinkVehicleMapper<TestLink> {

        public TestLinkCarMapper(String profileName) {
            super(profileName, TestLink.class);
        }

        public TestLinkCarMapper() {
            super("car", TestLink.class);
        }

        @Override
        public DirectionalDto<Boolean> getAccessibility(TestLink link) {
            return DirectionalDto.<Boolean>builder()
                    .forward(link.getSpeedInKilometersPerHour() > 0.0)
                    .reverse(link.getReverseSpeedInKilometersPerHour() > 0.0)
                    .build();
        }

        @Override
        public DirectionalDto<Double> getSpeed(TestLink link) {
            return DirectionalDto.<Double>builder()
                    .forward(link.getSpeedInKilometersPerHour())
                    .reverse(link.getReverseSpeedInKilometersPerHour())
                    .build();
        }
    }
}
