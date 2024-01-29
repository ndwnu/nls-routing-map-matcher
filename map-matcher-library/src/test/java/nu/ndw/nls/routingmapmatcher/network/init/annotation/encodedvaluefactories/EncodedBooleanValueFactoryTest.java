package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedBooleanValueFactoryTest {

    private static final String NAME_KEY = "key";

    @Mock
    private EncodedValueDto<?, Boolean> encodedValueDto;

    private final EncodedBooleanValueFactory encodedBooleanValueFactory = new EncodedBooleanValueFactory();

    @Test
    void getType_ok() {
        assertEquals(Boolean.class, encodedBooleanValueFactory.getType());
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
        EncodedValue encode = encodedBooleanValueFactory.encode(encodedValueDto);
        verify(encodedValueDto).key();
        verify(encodedValueDto).isDirectional();
        SimpleBooleanEncodedValue simpleBooleanEncodedValue = assertInstanceOf(SimpleBooleanEncodedValue.class, encode);
        assertEquals(NAME_KEY, simpleBooleanEncodedValue.getName());
        assertEquals(directional, simpleBooleanEncodedValue.isStoreTwoDirections());
    }


}