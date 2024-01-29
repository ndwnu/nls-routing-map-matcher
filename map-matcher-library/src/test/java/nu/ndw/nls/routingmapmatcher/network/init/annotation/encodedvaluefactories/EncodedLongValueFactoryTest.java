package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedLongValueFactoryTest {


    private static final String KEY = "key";
    @Mock
    private EncodedValueDto<?, Long> encodedValueDto;


    private final EncodedLongValueFactory encodedLongValueFactory = new EncodedLongValueFactory();

    @Test
    void getType() {
        assertEquals(Long.class, encodedLongValueFactory.getType());
    }

    @Test
    void encode_ok_directional() {
        encode(true);
    }

    @Test
    void encode_ok_nonDirectional() {
        encode(false);
    }

    private void encode(boolean directional) {
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.isDirectional()).thenReturn(directional);
        when(encodedValueDto.bits()).thenReturn(12);
        EncodedValue encode = encodedLongValueFactory.encode(encodedValueDto);
        verify(encodedValueDto).key();
        verify(encodedValueDto).isDirectional();
        verify(encodedValueDto).bits();

        IntEncodedValueImpl intEncodedValue = assertInstanceOf(IntEncodedValueImpl.class, encode);
        assertEquals(KEY, intEncodedValue.getName());
        assertEquals(directional, intEncodedValue.isStoreTwoDirections());
    }
}