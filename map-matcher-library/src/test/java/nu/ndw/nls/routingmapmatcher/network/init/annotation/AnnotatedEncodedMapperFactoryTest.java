package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.util.PMap;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedEncodedMapperFactoryTest {

    public static final String FIELD_KEY_BOOLEAN = "field_key_boolean";
    public static final String FIELD_KEY_STRING = "field_key_string";

    private static class MyLink extends Link {

        public MyLink(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
        }
    }

    @Mock
    private EncodedMapperFactoryRegistry  encodedMapperFactoryRegistry;
    @Mock
    private nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory<Boolean> booleanEncodedMapperFactory;

    @Mock
    private nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories.EncodedMapperFactory<String> stringEncodedMapperFactory;

    @Mock
    private EncodedValuesByTypeDto<MyLink> encodedValuesByTypeDto;

    @Mock
    private EncodedValueLookup encodedValueLookup;

    @Mock
    private PMap map;

    @Mock
    private AbstractEncodedMapper<MyLink, Boolean> booleanEncodedMapper;

    @Mock
    private AbstractEncodedMapper<MyLink, String> stringEncodedMapper;

    @Mock
    private EncodedValueDto<MyLink, Boolean> fieldKeyBooleanEncodedValueInstance;
    @Mock
    private EncodedValueDto<MyLink, String> fieldKeyStringEncodedValueInstance;


    @Test
    void create_ok_createsEncodedMappers() {
        when(encodedMapperFactoryRegistry.lookupEncodedMapperFactory(Boolean.class))
                .thenReturn(Optional.of(booleanEncodedMapperFactory));

        when(encodedMapperFactoryRegistry.lookupEncodedMapperFactory(String.class))
                .thenReturn(Optional.of(stringEncodedMapperFactory));

        when(encodedValuesByTypeDto.getValueTypeByKey(FIELD_KEY_BOOLEAN)).thenReturn(Optional.of(Boolean.class));
        when(encodedValuesByTypeDto.get(Boolean.class, FIELD_KEY_BOOLEAN)).thenReturn(
                fieldKeyBooleanEncodedValueInstance);
        when(encodedValuesByTypeDto.getValueTypeByKey(FIELD_KEY_STRING)).thenReturn(Optional.of(String.class));
        when(encodedValuesByTypeDto.get(String.class, FIELD_KEY_STRING)).thenReturn(fieldKeyStringEncodedValueInstance);

        when(booleanEncodedMapperFactory.create(encodedValueLookup, fieldKeyBooleanEncodedValueInstance)).thenReturn(
                booleanEncodedMapper);

        when(stringEncodedMapperFactory.create(encodedValueLookup, fieldKeyStringEncodedValueInstance)).thenReturn(
                stringEncodedMapper);

        AnnotatedEncodedMapperFactory<MyLink> annotatedEncodedMapperFactory =
                new AnnotatedEncodedMapperFactory<>(encodedMapperFactoryRegistry, encodedValuesByTypeDto);

        assertEquals(booleanEncodedMapper,
                annotatedEncodedMapperFactory.create(encodedValueLookup, FIELD_KEY_BOOLEAN, map));
        assertEquals(stringEncodedMapper, annotatedEncodedMapperFactory.create(encodedValueLookup, FIELD_KEY_STRING, map));
    }

    /**
     * This scenario can occur when an {@link nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue} is applied
     * on a type for which there is no encoded mapper factory in the spring boot context
     */
    @Test
    void create_fail_missingEncodedFactoryForType() {
        when(encodedMapperFactoryRegistry.lookupEncodedMapperFactory(Boolean.class))
                .thenReturn(Optional.empty());

        when(encodedValuesByTypeDto.getValueTypeByKey(FIELD_KEY_BOOLEAN)).thenReturn(Optional.of(Boolean.class));

        AnnotatedEncodedMapperFactory<MyLink> annotatedEncodedMapperFactory =
                new AnnotatedEncodedMapperFactory<>(encodedMapperFactoryRegistry, encodedValuesByTypeDto);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
            annotatedEncodedMapperFactory.create(encodedValueLookup, FIELD_KEY_BOOLEAN, map);
        });


        assertEquals("No tag parser found for name: field_key_boolean with type: class java.lang.Boolean",
                illegalStateException.getMessage());
    }

    /**
     * This scenario should never occur, because it means that GraphHopper tries to encode a value for which there
     * is no annotated configuration. If there is no annotated configuration, then the field shouldn't be encoded in
     * the first place.
     */
    @Test
    void create_fail_incorrectState() {
        when(encodedValuesByTypeDto.getValueTypeByKey(FIELD_KEY_BOOLEAN)).thenReturn(Optional.empty());

        AnnotatedEncodedMapperFactory<MyLink> annotatedEncodedMapperFactory =
                new AnnotatedEncodedMapperFactory<>(encodedMapperFactoryRegistry, encodedValuesByTypeDto);

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
            annotatedEncodedMapperFactory.create(encodedValueLookup, FIELD_KEY_BOOLEAN, map);
        });

        assertEquals("No annotate encoded value configuration found by name: field_key_boolean",
                illegalStateException.getMessage());
    }
}