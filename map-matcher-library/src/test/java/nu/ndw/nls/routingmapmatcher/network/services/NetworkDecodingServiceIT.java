package nu.ndw.nls.routingmapmatcher.network.services;

import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.BOOLEAN_DIRECTIONAL;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.BOOLEAN_PRIMITIVE;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.BOOLEAN_WRAPPER;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.DOUBLE_PRIMITIVE;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.DOUBLE_WRAPPER;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.INTEGER_DIRECTIONAL;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.INTEGER_PRIMITIVE;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.INTEGER_WRAPPER;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.LONG_DIRECTIONAL;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.LONG_PRIMITIVE;
import static nu.ndw.nls.routingmapmatcher.network.services.NetworkDecodingServiceIT.TestLink.STRING_WRAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import com.graphhopper.config.Profile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.network.GraphHopperNetworkService;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.decoding.services.NetworkDecodingService;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;
import nu.ndw.nls.springboot.test.logging.LoggerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class NetworkDecodingServiceIT {

    private static final String TEST_NETWORK = "test_network";
    private static final String LINK_2_WRAPPER = "link_2_wrapper";
    private static final String LINK_2_DIRECTIONAL_A = "link_2_directional_a";
    private static final String LINK_2_DIRECTIONAL_B = "link_2_directional_b";
    private static final String LINK_1_WRAPPER = "link_1_wrapper";
    private static final String LINK_1_DIRECTIONAL_A = "link_1_directional_a";
    private static final String LINK_1_DIRECTIONAL_B = "link_1_directional_b";
    private static final Instant DATA_DATE = Instant.parse("2023-11-07T15:37:23.129Z");
    private static final Instant DATA_DATE_TRUNCATED = Instant.parse("2023-11-07T15:37:23Z");

    private static final List<Profile> TEST_PROFILES = List.of(new Profile("car"));

    private final GeometryFactoryWgs84 geometryFactoryWgs84 = new GeometryFactoryWgs84();

    @Autowired
    private GraphHopperNetworkService graphHopperNetworkService;

    @Autowired
    private NetworkDecodingService networkDecodingService;

    @RegisterExtension
    public final LoggerExtension loggerExtension = new LoggerExtension();


    public static abstract class CarMapper<T extends Link> extends LinkVehicleMapper<T> {

        public CarMapper(Class<T> clazz) {
            super("car", clazz);
        }

        @Override
        public DirectionalDto<Boolean> getAccessibility(T link) {
            return DirectionalDto.<Boolean>builder()
                    .forward(true)
                    .reverse(true)
                    .build();
        }

        @Override
        public DirectionalDto<Double> getSpeed(T link) {
            return DirectionalDto.<Double>builder()
                    .forward(10D)
                    .reverse(10D)
                    .build();
        }
    }

    @Component
    public static class TestLinkCarMapper extends CarMapper<TestLink> {
        public TestLinkCarMapper() {
            super(TestLink.class);
        }
    }

    @Component
    public static class TestLinkEncodedValueMismatchEncodedValueParamNameInConstructorCarMapper extends
            CarMapper<TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor> {
        public TestLinkEncodedValueMismatchEncodedValueParamNameInConstructorCarMapper() {
            super(TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor.class);
        }
    }

    @Getter
    public static class TestLink extends Link {

        protected static final String BOOLEAN_WRAPPER = "boolean_wrapper";
        protected static final String BOOLEAN_PRIMITIVE = "boolean_primitive";
        protected static final String BOOLEAN_DIRECTIONAL = "boolean_directional";
        protected static final String STRING_WRAPPER = "string_wrapper";
        protected static final String STRING_DIRECTIONAL = "string_directional";
        protected static final String INTEGER_WRAPPER = "integer_wrapper";
        protected static final String INTEGER_PRIMITIVE = "integer_primitive";
        protected static final String INTEGER_DIRECTIONAL = "integer_directional";
        protected static final String LONG_WRAPPER = "long_wrapper";
        protected static final String LONG_PRIMITIVE = "long_primitive";
        protected static final String LONG_DIRECTIONAL = "long_directional";
        protected static final String DOUBLE_WRAPPER = "double_wrapper";
        protected static final String DOUBLE_PRIMITIVE = "double_primitive";
        protected static final String DOUBLE_DIRECTIONAL = "double_directional";

        @EncodedValue(key = BOOLEAN_WRAPPER, bits = 1)
        private final Boolean extraBooleanWrapper;

        @EncodedValue(key = BOOLEAN_PRIMITIVE, bits = 1)
        private final Boolean extraBooleanPrimitive;

        @EncodedValue(key = BOOLEAN_DIRECTIONAL, bits = 1)
        private final DirectionalDto<Boolean> extraBooleanDirectional;

        @EncodedValue(key = INTEGER_WRAPPER, bits = 31)
        private final Integer extraIntegerWrapper;

        @EncodedValue(key = INTEGER_PRIMITIVE, bits = 31)
        private final int extraIntegerPrimitive;

        @EncodedValue(key = INTEGER_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Integer> extraIntegerDirectional;

        @EncodedValue(key = LONG_WRAPPER, bits = 31)
        private final Long extraLongWrapper;

        @EncodedValue(key = LONG_PRIMITIVE, bits = 31)
        private final long extraLongPrimitive;

        @EncodedValue(key = LONG_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Long> extraLongDirectional;

        @EncodedValue(key = DOUBLE_WRAPPER, bits = 31)
        private final Double extraDoubleWrapper;

        @EncodedValue(key = DOUBLE_PRIMITIVE, bits = 31)
        private final double extraDoublePrimitive;

        @EncodedValue(key = DOUBLE_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Double> extraDoubleDirectional;

        @EncodedValue(key = STRING_WRAPPER, bits = 2)
        private final String extraStringWrapper;

        @EncodedValue(key = STRING_DIRECTIONAL, bits = 4)
        private final DirectionalDto<String> extraStringDirectional;

        @Builder
        public TestLink(long id, long fromNodeId, long toNodeId,
                double distanceInMeters, Boolean extraBooleanWrapper, LineString geometry,
                Boolean extraBooleanPrimitive, DirectionalDto<Boolean> extraBooleanDirectional, Integer extraIntegerWrapper,
                Integer extraIntegerPrimitive, DirectionalDto<Integer> extraIntegerDirectional, Long extraLongWrapper,
                Long extraLongPrimitive, DirectionalDto<Long> extraLongDirectional, Double extraDoubleWrapper, Double extraDoublePrimitive,
                DirectionalDto<Double> extraDoubleDirectional, String extraStringWrapper, DirectionalDto<String> extraStringDirectional) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
            this.extraBooleanWrapper = extraBooleanWrapper;
            this.extraBooleanPrimitive = extraBooleanPrimitive;
            this.extraBooleanDirectional = extraBooleanDirectional;
            this.extraIntegerWrapper = extraIntegerWrapper;
            this.extraIntegerPrimitive = extraIntegerPrimitive;
            this.extraIntegerDirectional = extraIntegerDirectional;
            this.extraLongWrapper = extraLongWrapper;
            this.extraLongPrimitive = extraLongPrimitive;
            this.extraLongDirectional = extraLongDirectional;
            this.extraDoubleWrapper = extraDoubleWrapper;
            this.extraDoublePrimitive = extraDoublePrimitive;
            this.extraDoubleDirectional = extraDoubleDirectional;
            this.extraStringWrapper = extraStringWrapper;
            this.extraStringDirectional = extraStringDirectional;
        }
    }

    @Getter
    public static class TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor extends Link {

        protected static final String BOOLEAN_WRAPPER = "boolean_wrapper";

        @EncodedValue(key = BOOLEAN_WRAPPER, bits = 1)
        private final Boolean extraBooleanWrapper;

        @Builder
        public TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor(long id, long fromNodeId, long toNodeId,
                double distanceInMeters, LineString geometry, Boolean nonMatchingConstructorParamName) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
            // Encoded value member name is different from constructor argument, this is not allowed for decoding, because we won't be
            // able to figure out which constructor arguments match the encoded values
            this.extraBooleanWrapper = nonMatchingConstructorParamName;
        }
    }

    @SneakyThrows
    @Test
    void decodeByRoadSectionId_ok_oneEncodedValueNotAccessibleInConstructor() {
        Path tempDirectory = Files.createTempDirectory("graphhopper");
        RoutingNetworkSettings<TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor> routingNetworkSettings =
                RoutingNetworkSettings.builder(TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor.class)
                        .networkNameAndVersion(TEST_NETWORK)
                        .profiles(TEST_PROFILES)
                        .graphhopperRootPath(tempDirectory)
                        .linkSupplier(() -> List.of(
                                TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor.builder()
                                        .id(1)
                                        .distanceInMeters(1)
                                        .geometry(geometryFactoryWgs84.createLineString(new Coordinate[]{new Coordinate(1, 2),
                                                new Coordinate(3, 4)}))
                                        .fromNodeId(1)
                                        .toNodeId(2)
                                        .build()).iterator())
                        .dataDate(DATA_DATE)
                        .indexed(true)
                        .build();

        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        assertThatThrownBy(() ->
                networkDecodingService.decodeByRoadSectionId(networkGraphHopper, 1L,
                        TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to find a suitable constructor! Please verify that the "
                        + "TestLinkEncodedValueMismatchEncodedValueParamNameInConstructor constructors arguments contain the default Link "
                        + "property names: [distanceInMeters, fromNodeId, geometry, toNodeId] and that all EncodedValue annotated "
                        + "properties are using the same argument names in the constructor: [extraBooleanWrapper, id, linkIdReversed]");


        loggerExtension.containsLog(Level.DEBUG, "Matched 5 encoded values, missing [nonMatchingConstructorParamName]");
        loggerExtension.containsLog(Level.DEBUG, "Found 0 constructors that match all encoded value arguments");
    }

    @SneakyThrows
    @Test
    void decodeByRoadSectionId_ok_capableOfDecodingAllPossibleJavaTypesIntoALinkObject() {

        LineString lineString1 = geometryFactoryWgs84.createLineString(new Coordinate[]{new Coordinate(1, 2),
                new Coordinate(3, 4)});

        LineString lineString2 = geometryFactoryWgs84.createLineString(new Coordinate[]{new Coordinate(5, 6),
                new Coordinate(7, 8)});

        Path tempDirectory = Files.createTempDirectory("graphhopper");
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(tempDirectory)
                .linkSupplier(() -> List.of(
                        TestLink.builder()
                                .id(1)
                                .distanceInMeters(100)
                                .geometry(lineString1)
                                .fromNodeId(1)
                                .toNodeId(2)
                                .extraBooleanWrapper(true)
                                .extraBooleanPrimitive(false)
                                .extraBooleanDirectional(new DirectionalDto<>(true, false))
                                .extraStringWrapper(LINK_1_WRAPPER)
                                .extraStringDirectional(new DirectionalDto<>(LINK_1_DIRECTIONAL_A, LINK_1_DIRECTIONAL_B))
                                .extraIntegerWrapper(1)
                                .extraIntegerPrimitive(2)
                                .extraIntegerDirectional(new DirectionalDto<>(3, 4))
                                .extraLongWrapper(9L)
                                .extraLongPrimitive(10L)
                                .extraLongDirectional(new DirectionalDto<>(11L, 12L))
                                .extraDoubleWrapper(17.0)
                                .extraDoublePrimitive(18.0)
                                .extraDoubleDirectional(new DirectionalDto<>(19.0, 20.0))
                                .build(),
                        TestLink.builder()
                                .id(2)
                                .distanceInMeters(200)
                                .geometry(lineString2)
                                .extraBooleanWrapper(false)
                                .extraBooleanPrimitive(true)
                                .extraBooleanDirectional(new DirectionalDto<>(false, true))
                                .extraStringWrapper(LINK_2_WRAPPER)
                                .extraStringDirectional(new DirectionalDto<>(LINK_2_DIRECTIONAL_A, LINK_2_DIRECTIONAL_B))
                                .extraIntegerWrapper(5)
                                .extraIntegerPrimitive(6)
                                .extraIntegerDirectional(new DirectionalDto<>(7, 8))
                                .extraLongWrapper(13L)
                                .extraLongPrimitive(14L)
                                .extraLongDirectional(new DirectionalDto<>(15L, 16L))
                                .extraDoubleWrapper(21.0)
                                .extraDoublePrimitive(22.0)
                                .extraDoubleDirectional(new DirectionalDto<>(23.0, 24.0))
                                .fromNodeId(3)
                                .toNodeId(4)
                                
                                .build()).iterator())
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);

        TestLink decodedLinkId1 = networkDecodingService.decodeByRoadSectionId(networkGraphHopper, 1L, TestLink.class);
        assertThat(decodedLinkId1).isNotNull();
        assertThat(decodedLinkId1.getId()).isEqualTo(1);

        assertThat(decodedLinkId1.getExtraBooleanWrapper()).isTrue();
        assertThat(decodedLinkId1.getExtraBooleanPrimitive()).isFalse();
        assertThat(decodedLinkId1.getExtraBooleanDirectional()).isEqualTo(new DirectionalDto<>(true, false));

        assertThat(decodedLinkId1.getExtraIntegerWrapper()).isEqualTo(1);
        assertThat(decodedLinkId1.getExtraIntegerPrimitive()).isEqualTo(2);
        assertThat(decodedLinkId1.getExtraIntegerDirectional()).isEqualTo(new DirectionalDto<>(3, 4));

        assertThat(decodedLinkId1.getExtraLongWrapper()).isEqualTo(9L);
        assertThat(decodedLinkId1.getExtraLongPrimitive()).isEqualTo(10L);
        assertThat(decodedLinkId1.getExtraLongDirectional()).isEqualTo(new DirectionalDto<>(11L, 12L));

        assertThat(decodedLinkId1.getExtraDoubleWrapper()).isEqualTo(17.0);
        assertThat(decodedLinkId1.getExtraDoublePrimitive()).isEqualTo(18.0);
        assertThat(decodedLinkId1.getExtraDoubleDirectional()).isEqualTo(new DirectionalDto<>(19.0, 20.0));

        assertThat(decodedLinkId1.getExtraStringWrapper()).isEqualTo(LINK_1_WRAPPER);
        assertThat(decodedLinkId1.getExtraStringDirectional()).isEqualTo(new DirectionalDto<>(LINK_1_DIRECTIONAL_A, LINK_1_DIRECTIONAL_B));

        assertThat(decodedLinkId1.getDistanceInMeters()).isEqualTo(100);
        assertThat(decodedLinkId1.getGeometry()).isEqualTo(lineString1);


        TestLink decodedLinkId2 = networkDecodingService.decodeByRoadSectionId(networkGraphHopper, 2L, TestLink.class);
        assertThat(decodedLinkId2.getId()).isEqualTo(2);
        assertThat(decodedLinkId2).isNotNull();

        assertThat(decodedLinkId2.getExtraBooleanWrapper()).isFalse();
        assertThat(decodedLinkId2.getExtraBooleanPrimitive()).isTrue();
        assertThat(decodedLinkId2.getExtraBooleanDirectional()).isEqualTo(new DirectionalDto<>(false, true));

        assertThat(decodedLinkId2.getExtraIntegerWrapper()).isEqualTo(5);
        assertThat(decodedLinkId2.getExtraIntegerPrimitive()).isEqualTo(6);
        assertThat(decodedLinkId2.getExtraIntegerDirectional()).isEqualTo(new DirectionalDto<>(7, 8));

        assertThat(decodedLinkId2.getExtraLongWrapper()).isEqualTo(13L);
        assertThat(decodedLinkId2.getExtraLongPrimitive()).isEqualTo(14L);
        assertThat(decodedLinkId2.getExtraLongDirectional()).isEqualTo(new DirectionalDto<>(15L, 16L));

        assertThat(decodedLinkId2.getExtraDoubleWrapper()).isEqualTo(21.0);
        assertThat(decodedLinkId2.getExtraDoublePrimitive()).isEqualTo(22.0);
        assertThat(decodedLinkId2.getExtraDoubleDirectional()).isEqualTo(new DirectionalDto<>(23.0, 24.0));

        assertThat(decodedLinkId2.getExtraStringWrapper()).isEqualTo(LINK_2_WRAPPER);
        assertThat(decodedLinkId2.getExtraStringDirectional()).isEqualTo(new DirectionalDto<>(LINK_2_DIRECTIONAL_A, LINK_2_DIRECTIONAL_B));

        assertThat(decodedLinkId2.getDistanceInMeters()).isEqualTo(200);
        assertThat(decodedLinkId2.getGeometry()).isEqualTo(lineString2);

        loggerExtension.containsLog(Level.DEBUG, "Matched 19 encoded values, missing []");
        loggerExtension.containsLog(Level.DEBUG, "Found 1 constructors that match all encoded value arguments");
    }


    @Getter
    public static class SomeNetworkDecodedPropertiesDto implements NetworkEncoded {
        @EncodedValue(key = TestLink.INTEGER_PRIMITIVE)
        private final int someIntegerValue;

        @EncodedValue(key = LONG_DIRECTIONAL)
        private final DirectionalDto<Long> someDirectionalLongValue;

        public SomeNetworkDecodedPropertiesDto(int someIntegerValue, DirectionalDto<Long> someDirectionalLongValue) {
            this.someIntegerValue = someIntegerValue;
            this.someDirectionalLongValue = someDirectionalLongValue;
        }
    }

    @Test
    @SneakyThrows
    void decodeByRoadSectionId_ok_decodingPartiallyIntoNonLinkDto() {
        LineString lineString1 = geometryFactoryWgs84.createLineString(new Coordinate[]{new Coordinate(1, 2),
                new Coordinate(3, 4)});

        Path tempDirectory = Files.createTempDirectory("graphhopper");
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(tempDirectory)
                .linkSupplier(() -> List.of(
                        TestLink.builder()
                                .id(1)
                                .distanceInMeters(100)
                                .geometry(lineString1)
                                .fromNodeId(1)
                                .toNodeId(2)
                                .extraBooleanWrapper(true)
                                .extraBooleanPrimitive(false)
                                .extraBooleanDirectional(new DirectionalDto<>(true, false))
                                .extraStringWrapper(LINK_1_WRAPPER)
                                .extraStringDirectional(new DirectionalDto<>(LINK_1_DIRECTIONAL_A, LINK_1_DIRECTIONAL_B))
                                .extraIntegerWrapper(1)
                                .extraIntegerPrimitive(2)
                                .extraIntegerDirectional(new DirectionalDto<>(3, 4))
                                .extraLongWrapper(9L)
                                .extraLongPrimitive(10L)
                                .extraLongDirectional(new DirectionalDto<>(11L, 12L))
                                .extraDoubleWrapper(17.0)
                                .extraDoublePrimitive(18.0)
                                .extraDoubleDirectional(new DirectionalDto<>(19.0, 20.0))
                                .build()).iterator())
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);


        SomeNetworkDecodedPropertiesDto someNetworkDecodedPropertiesDto =
                networkDecodingService.decodeByRoadSectionId(networkGraphHopper, 1L, SomeNetworkDecodedPropertiesDto.class);

        assertThat(someNetworkDecodedPropertiesDto).isNotNull();
        assertThat(someNetworkDecodedPropertiesDto.getSomeIntegerValue()).isEqualTo(2);
        assertThat(someNetworkDecodedPropertiesDto.getSomeDirectionalLongValue()).isEqualTo(new DirectionalDto<>(11L, 12L));
    }


    @Getter
    public static class AllPropertiesDto implements NetworkEncoded{
        @EncodedValue(key = WAY_ID_KEY, bits = 31)
        private final long id;

        @EncodedValue(key = BOOLEAN_WRAPPER, bits = 1)
        private final Boolean extraBooleanWrapper;

        @EncodedValue(key = BOOLEAN_PRIMITIVE, bits = 1)
        private final Boolean extraBooleanPrimitive;

        @EncodedValue(key = BOOLEAN_DIRECTIONAL, bits = 1)
        private final DirectionalDto<Boolean> extraBooleanDirectional;

        @EncodedValue(key = INTEGER_WRAPPER, bits = 31)
        private final Integer extraIntegerWrapper;

        @EncodedValue(key = INTEGER_PRIMITIVE, bits = 31)
        private final int extraIntegerPrimitive;

        @EncodedValue(key = INTEGER_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Integer> extraIntegerDirectional;

        @EncodedValue(key = TestLink.LONG_WRAPPER, bits = 31)
        private final Long extraLongWrapper;

        @EncodedValue(key = LONG_PRIMITIVE, bits = 31)
        private final long extraLongPrimitive;

        @EncodedValue(key = LONG_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Long> extraLongDirectional;

        @EncodedValue(key = DOUBLE_WRAPPER, bits = 31)
        private final Double extraDoubleWrapper;

        @EncodedValue(key = DOUBLE_PRIMITIVE, bits = 31)
        private final double extraDoublePrimitive;

        @EncodedValue(key = TestLink.DOUBLE_DIRECTIONAL, bits = 31)
        private final DirectionalDto<Double> extraDoubleDirectional;

        @EncodedValue(key = STRING_WRAPPER, bits = 2)
        private final String extraStringWrapper;

        @EncodedValue(key = TestLink.STRING_DIRECTIONAL, bits = 4)
        private final DirectionalDto<String> extraStringDirectional;

        // Does not need annotation, because it's a core link property. The name needs to exactly "distanceInMeters"
        private final double distanceInMeters;

        // Does not need annotation, because it's a core link property. The name needs to exactly "geometry"
        private final LineString geometry;

        public AllPropertiesDto(long id, Boolean extraBooleanWrapper, Boolean extraBooleanPrimitive, DirectionalDto<Boolean> extraBooleanDirectional,
                Integer extraIntegerWrapper, int extraIntegerPrimitive, DirectionalDto<Integer> extraIntegerDirectional,
                Long extraLongWrapper, long extraLongPrimitive, DirectionalDto<Long> extraLongDirectional, Double extraDoubleWrapper,
                double extraDoublePrimitive, DirectionalDto<Double> extraDoubleDirectional, String extraStringWrapper,
                DirectionalDto<String> extraStringDirectional, double distanceInMeters, LineString geometry) {
            this.id = id;
            this.extraBooleanWrapper = extraBooleanWrapper;
            this.extraBooleanPrimitive = extraBooleanPrimitive;
            this.extraBooleanDirectional = extraBooleanDirectional;
            this.extraIntegerWrapper = extraIntegerWrapper;
            this.extraIntegerPrimitive = extraIntegerPrimitive;
            this.extraIntegerDirectional = extraIntegerDirectional;
            this.extraLongWrapper = extraLongWrapper;
            this.extraLongPrimitive = extraLongPrimitive;
            this.extraLongDirectional = extraLongDirectional;
            this.extraDoubleWrapper = extraDoubleWrapper;
            this.extraDoublePrimitive = extraDoublePrimitive;
            this.extraDoubleDirectional = extraDoubleDirectional;
            this.extraStringWrapper = extraStringWrapper;
            this.extraStringDirectional = extraStringDirectional;
            this.distanceInMeters = distanceInMeters;
            this.geometry = geometry;
        }
    }

    @Test
    @SneakyThrows
    void decodeByRoadSectionId_ok_decodingAllPropertiesToCustomDto() {

        LineString lineString1 = geometryFactoryWgs84.createLineString(new Coordinate[]{new Coordinate(1, 2),
                new Coordinate(3, 4)});

        Path tempDirectory = Files.createTempDirectory("graphhopper");
        RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.builder(TestLink.class)
                .networkNameAndVersion(TEST_NETWORK)
                .profiles(TEST_PROFILES)
                .graphhopperRootPath(tempDirectory)
                .linkSupplier(() -> List.of(
                        TestLink.builder()
                                .id(1)
                                .distanceInMeters(100)
                                .geometry(lineString1)
                                .fromNodeId(1)
                                .toNodeId(2)
                                .extraBooleanWrapper(true)
                                .extraBooleanPrimitive(false)
                                .extraBooleanDirectional(new DirectionalDto<>(true, false))
                                .extraStringWrapper(LINK_1_WRAPPER)
                                .extraStringDirectional(new DirectionalDto<>(LINK_1_DIRECTIONAL_A, LINK_1_DIRECTIONAL_B))
                                .extraIntegerWrapper(1)
                                .extraIntegerPrimitive(2)
                                .extraIntegerDirectional(new DirectionalDto<>(3, 4))
                                .extraLongWrapper(9L)
                                .extraLongPrimitive(10L)
                                .extraLongDirectional(new DirectionalDto<>(11L, 12L))
                                .extraDoubleWrapper(17.0)
                                .extraDoublePrimitive(18.0)
                                .extraDoubleDirectional(new DirectionalDto<>(19.0, 20.0))
                                .build()).iterator())
                .dataDate(DATA_DATE)
                .indexed(true)
                .build();

        graphHopperNetworkService.storeOnDisk(routingNetworkSettings);
        NetworkGraphHopper networkGraphHopper = graphHopperNetworkService.loadFromDisk(routingNetworkSettings);
        assertThat(networkGraphHopper).isNotNull();
        assertThat(networkGraphHopper.getImportDate()).isNotNull();
        assertThat(networkGraphHopper.getDataDate()).isEqualTo(DATA_DATE_TRUNCATED);


        AllPropertiesDto decodedLinkId1 = networkDecodingService.decodeByRoadSectionId(networkGraphHopper, 1L, AllPropertiesDto.class);
        assertThat(decodedLinkId1).isNotNull();
        assertThat(decodedLinkId1.getId()).isEqualTo(1);

        assertThat(decodedLinkId1.getExtraBooleanWrapper()).isTrue();
        assertThat(decodedLinkId1.getExtraBooleanPrimitive()).isFalse();
        assertThat(decodedLinkId1.getExtraBooleanDirectional()).isEqualTo(new DirectionalDto<>(true, false));

        assertThat(decodedLinkId1.getExtraIntegerWrapper()).isEqualTo(1);
        assertThat(decodedLinkId1.getExtraIntegerPrimitive()).isEqualTo(2);
        assertThat(decodedLinkId1.getExtraIntegerDirectional()).isEqualTo(new DirectionalDto<>(3, 4));

        assertThat(decodedLinkId1.getExtraLongWrapper()).isEqualTo(9L);
        assertThat(decodedLinkId1.getExtraLongPrimitive()).isEqualTo(10L);
        assertThat(decodedLinkId1.getExtraLongDirectional()).isEqualTo(new DirectionalDto<>(11L, 12L));

        assertThat(decodedLinkId1.getExtraDoubleWrapper()).isEqualTo(17.0);
        assertThat(decodedLinkId1.getExtraDoublePrimitive()).isEqualTo(18.0);
        assertThat(decodedLinkId1.getExtraDoubleDirectional()).isEqualTo(new DirectionalDto<>(19.0, 20.0));

        assertThat(decodedLinkId1.getExtraStringWrapper()).isEqualTo(LINK_1_WRAPPER);
        assertThat(decodedLinkId1.getExtraStringDirectional()).isEqualTo(new DirectionalDto<>(LINK_1_DIRECTIONAL_A, LINK_1_DIRECTIONAL_B));

        assertThat(decodedLinkId1.getDistanceInMeters()).isEqualTo(100);
        assertThat(decodedLinkId1.getGeometry()).isEqualTo(lineString1);

    }

}