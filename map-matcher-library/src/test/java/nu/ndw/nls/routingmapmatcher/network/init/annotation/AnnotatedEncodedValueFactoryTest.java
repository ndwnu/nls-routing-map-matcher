package nu.ndw.nls.routingmapmatcher.network.init.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.util.PMap;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValuesByTypeDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories.EncodedValueFactory;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.LineString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnnotatedEncodedValueFactoryTest {

    public static final String VALUE_KEY_A = "value_key_a";

    private static class MyLink extends Link {
        protected MyLink(long id, long fromNodeId, long toNodeId, double distanceInMeters,
                LineString geometry) {
            super(id, fromNodeId, toNodeId, distanceInMeters, geometry);
        }
    }

    @Mock
    private EncodedValueFactoryRegistry encodedValueFactoryRegistry;

    @Mock
    private EncodedValuesByTypeDto<MyLink> encodedValuesByTypeDto;

    @Mock
    private EncodedValueDto<MyLink, String> encodedValueDtoString;

    @Mock
    private EncodedValueFactory<String> stringEncodedValueFactory;

    @Mock
    private PMap map;

    @InjectMocks
    private AnnotatedEncodedValueFactory<MyLink> annotatedEncodedValueFactory;


    @Test
    void create_ok_encodesType() {
        when(encodedValuesByTypeDto.getValueTypeByKey(VALUE_KEY_A))
                .thenReturn(Optional.of(String.class));
        when(encodedValuesByTypeDto.get(String.class, VALUE_KEY_A)).thenReturn(encodedValueDtoString);

        when(encodedValueFactoryRegistry.lookupEncodedValueFactory(String.class))
                .thenReturn(Optional.of(stringEncodedValueFactory));

        annotatedEncodedValueFactory.create(VALUE_KEY_A, map);

        verify(stringEncodedValueFactory).encode(encodedValueDtoString);
    }

    @Test
    void create_fail_noEncodedValueDtoFound() {
        when(encodedValuesByTypeDto.getValueTypeByKey(VALUE_KEY_A))
                .thenReturn(Optional.empty());

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> annotatedEncodedValueFactory.create(VALUE_KEY_A, map));

        assertEquals("Field requested for which there is no encoded value annotation: value_key_a",
                illegalStateException.getMessage());

        verify(stringEncodedValueFactory,  never()).encode(encodedValueDtoString);
    }

    @Test
    void create_fail_noEncodedValueFactoryFoundForType() {
        when(encodedValuesByTypeDto.getValueTypeByKey(VALUE_KEY_A))
                .thenReturn(Optional.of(String.class));
        when(encodedValuesByTypeDto.get(String.class, VALUE_KEY_A)).thenReturn(encodedValueDtoString);

        when(encodedValueFactoryRegistry.lookupEncodedValueFactory(String.class))
                .thenReturn(Optional.empty());

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class,
                () -> annotatedEncodedValueFactory.create(VALUE_KEY_A, map));

        assertEquals("No encoded value factory found for name: value_key_a with type: class java.lang.String",
                illegalStateException.getMessage());


        verify(stringEncodedValueFactory,never()).encode(encodedValueDtoString);
    }
}