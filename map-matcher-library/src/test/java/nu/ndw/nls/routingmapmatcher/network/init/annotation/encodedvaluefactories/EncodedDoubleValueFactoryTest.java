package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedDoubleValueFactoryTest {

    private static final String KEY = "key";
    private static final int BITS = 12;
    @Mock
    private EncodedValueDto<?, Double> encodedValueDto;

    private final EncodedDoubleValueFactory encodedDoubleValueFactory = new EncodedDoubleValueFactory();
    @Test
    void getType_ok() {
        assertEquals(Double.class, encodedDoubleValueFactory.getType());
    }
    @Test
    void encode_ok_storeDirectional() {
        encode(true);
    }

    @Test
    void encode_ok_storeNonDirectional() {
        encode(false);
    }

    private void encode(boolean directional) {
        when(encodedValueDto.key()).thenReturn(KEY);
        when(encodedValueDto.isDirectional()).thenReturn(directional);
        when(encodedValueDto.bits()).thenReturn(BITS);
        EncodedValue encode = encodedDoubleValueFactory.encode(encodedValueDto);
        verify(encodedValueDto).key();
        verify(encodedValueDto).isDirectional();
        verify(encodedValueDto).bits();
        DecimalEncodedValueImpl decimalEncodedValue = assertInstanceOf(DecimalEncodedValueImpl.class, encode);
        assertEquals(KEY, decimalEncodedValue.getName());
        assertEquals(0.0, decimalEncodedValue.getMinStorableDecimal());
        assertEquals(Double.POSITIVE_INFINITY, decimalEncodedValue.getMaxStorableDecimal());
    }
}


