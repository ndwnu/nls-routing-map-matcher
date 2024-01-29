package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedStringValueFactoryTest {

    private static final String NAME_KEY = "key";

    @Mock
    private EncodedValueDto<?, String> encodedValueDto;

    private final EncodedStringValueFactory encodedStringValueFactory = new EncodedStringValueFactory();

    @Test
    void getType_ok() {
        assertEquals(String.class, encodedStringValueFactory.getType());
    }

    @Test
    void encode_ok_storeDirectional() {
        encode(true);
    }

    @Test
    void encode_ok_storeNonDirection() {
        encode(false);
    }

    private void encode(Boolean directional) {
        when(encodedValueDto.key()).thenReturn(NAME_KEY);
        when(encodedValueDto.isDirectional()).thenReturn(directional);
        EncodedValue encode = encodedStringValueFactory.encode(encodedValueDto);
        verify(encodedValueDto).key();
        verify(encodedValueDto).isDirectional();
        StringEncodedValue stringEncodedValue = assertInstanceOf(StringEncodedValue.class, encode);
        assertEquals(NAME_KEY, stringEncodedValue.getName());
        assertEquals(directional, stringEncodedValue.isStoreTwoDirections());
    }
}