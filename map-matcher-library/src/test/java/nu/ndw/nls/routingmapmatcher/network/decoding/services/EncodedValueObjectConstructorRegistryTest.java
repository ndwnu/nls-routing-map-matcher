package nu.ndw.nls.routingmapmatcher.network.decoding.services;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.DirectionalFieldGenericTypeArgumentMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.mappers.EncodedValuesMapper;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.decoding.link.NonEncodedValueLinkPropertyRegistry;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.springboot.test.logging.LoggerExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedValueObjectConstructorRegistryTest {

    @Mock
    private DirectionalFieldGenericTypeArgumentMapper directionalFieldGenericTypeArgumentMapper;

    @Mock
    private NonEncodedValueLinkPropertyRegistry nonEncodedValueLinkPropertyRegistry;

    @Mock
    private EncodedValuesMapper encodedValuesMapper;

    @InjectMocks
    private EncodedValueObjectConstructorRegistry encodedValueObjectConstructorRegistry;

    @Mock
    private EncodedValuesByTypeDto<TestLink> encodedValuesByType;

    @Mock
    private EncodedValuesByTypeDto<TestLink2> encodedValuesByType2;

    @Mock
    private EncodedValuesByTypeDto<TestDto> encodedValuesByType3;

    @RegisterExtension
    public final LoggerExtension loggerExtension = new LoggerExtension();

    private static class TestLink extends Link {

        @EncodedValue(key = "my_key", bits = 10)
        private final int customEncodedValue;

        @EncodedValue(key = "my_key", bits = 10)
        private final DirectionalDto<Integer> customEncodedDirectionalValue;

        public TestLink(long id) {
            super(0, 0, 0, 0, null);
            this.customEncodedValue = 0;
            this.customEncodedDirectionalValue = null;
        }

        public TestLink(long id, long fromNodeId) {
            super(0, 0, 0, 0, null);
            this.customEncodedValue = 0;
            this.customEncodedDirectionalValue = null;
        }


        public TestLink(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry, int customEncodedValue,
                DirectionalDto<Integer> customEncodedDirectionalValue) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
            this.customEncodedValue = customEncodedValue;
            this.customEncodedDirectionalValue = customEncodedDirectionalValue;
        }
    }

    @Test
    void resolveConstructor_ok() {
        doReturn(Integer.class).when(directionalFieldGenericTypeArgumentMapper).map(TestLink.class, "customEncodedDirectionalValue");

        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("id", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("fromNodeId", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("toNodeId", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("distanceInMeters", double.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("geometry", LineString.class)).thenReturn(true);


        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("customEncodedValue", int.class)).thenReturn(false);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("customEncodedDirectionalValue", DirectionalDto.class)).thenReturn(false);

        when(encodedValuesMapper.map(TestLink.class)).thenReturn(encodedValuesByType);

        // Is using boxed types
        when(encodedValuesByType.getByProperty(Integer.class, "customEncodedValue")).thenReturn(Optional.of(
                EncodedValueDto.<TestLink, Integer>builder()
                        .build()));
        when(encodedValuesByType.getByProperty(Integer.class, "customEncodedDirectionalValue")).thenReturn(Optional.of(
                EncodedValueDto.<TestLink, Integer>builder()
                        .build()));

        Constructor<TestLink> testLinkConstructor = encodedValueObjectConstructorRegistry.resolveConstructor(TestLink.class);
        assertThat(testLinkConstructor.getParameterCount()).isEqualTo(7);

        loggerExtension.containsLog(Level.DEBUG, "Matched 7 encoded values, missing []");
        loggerExtension.containsLog(Level.DEBUG, "Found 3 constructors that match all encoded value arguments");
    }


    private static class TestLink2 extends Link {

        @EncodedValue(key = "my_key", bits = 10)
        private final int customEncodedValue;

        public TestLink2(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry,
                int constructorParameterDoesNotMatchMemberProperty) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
            this.customEncodedValue = constructorParameterDoesNotMatchMemberProperty;
        }
    }

    @Test
    void resolveConstructor_fail_constructorParameterDoesNotMatchMemberPropertyName() {

        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("id", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("fromNodeId", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("toNodeId", long.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("distanceInMeters", double.class)).thenReturn(true);
        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("geometry", LineString.class)).thenReturn(true);

        when(nonEncodedValueLinkPropertyRegistry.isNonEncodedProperty("constructorParameterDoesNotMatchMemberProperty", int.class))
                .thenReturn(false);

        when(encodedValuesMapper.map(TestLink2.class)).thenReturn(encodedValuesByType2);

        when(nonEncodedValueLinkPropertyRegistry.getNonEncodedProperties()).thenReturn(List.of("id", "fromNodeId", "toNodeId",
                "distanceInMeters", "geometry"));

        // Is using boxed types
        when(encodedValuesByType2.getByProperty(Integer.class, "constructorParameterDoesNotMatchMemberProperty")).thenReturn(
                Optional.empty());        // Is using boxed types
        when(encodedValuesByType2.getPropertyNameKeySet()).thenReturn(Set.of("constructorParameterDoesNotMatchMemberProperty"));

        assertThatThrownBy(() -> encodedValueObjectConstructorRegistry.resolveConstructor(TestLink2.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to find a suitable constructor! Please verify that the TestLink2 constructors arguments contain the "
                        + "default Link property names: [id, fromNodeId, toNodeId, distanceInMeters, geometry] and that all EncodedValue"
                        + " annotated properties are using the same argument names in the constructor: "
                        + "[constructorParameterDoesNotMatchMemberProperty]");
    }

    private static class TestDto implements NetworkEncoded {

        private TestDto(int unknownProperty) {
        }

    }

    @Test
    void resolveConstructor_fail_constructorParameterUnknown() {
        when(encodedValuesMapper.map(TestDto.class)).thenReturn(encodedValuesByType3);

            assertThatThrownBy(() -> encodedValueObjectConstructorRegistry.resolveConstructor(TestDto.class))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Failed to find a suitable constructor! Please verify that the TestDto constructors arguments contain the "
                            + "default Link property names: [] and that all EncodedValue"
                            + " annotated properties are using the same argument names in the constructor: "
                            + "[]");

        loggerExtension.containsLog(Level.DEBUG, "Matched 0 encoded values, missing [unknownProperty]");
        loggerExtension.containsLog(Level.DEBUG, "Found 0 constructors that match all encoded value arguments");
    }
}