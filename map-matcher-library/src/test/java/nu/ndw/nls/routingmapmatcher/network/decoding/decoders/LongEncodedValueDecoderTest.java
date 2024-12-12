package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LongEncodedValueDecoderTest {

    @Mock
    private IntegerEncodedValueDecoder integerEncodedValueDecoder;

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @InjectMocks
    private LongEncodedValueDecoder longEncodedValueDecoder;

    @Test
    void decode_ok() {
        when(integerEncodedValueDecoder.decode(networkGraphHopper, 10L, "encodedValueName", true)).thenReturn(10);
        assertThat(longEncodedValueDecoder.decode(networkGraphHopper, 10L, "encodedValueName", true)).isEqualTo(10L);
    }
}