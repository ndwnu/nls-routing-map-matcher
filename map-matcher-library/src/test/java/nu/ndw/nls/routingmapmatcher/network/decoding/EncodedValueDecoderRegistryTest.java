package nu.ndw.nls.routingmapmatcher.network.decoding;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueDistanceDecoder;
import nu.ndw.nls.routingmapmatcher.network.decoding.decoders.EncodedValueGeometryDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EncodedValueDecoderRegistryTest {

    @Mock
    private EncodedValueDecoder<Boolean> booleanEncodedValueDecoder;

    @Mock
    private EncodedValueDecoder<Double> doubleEncodedValueDecoder;

    @Mock
    private EncodedValueDistanceDecoder encodedValueDistanceDecoder;

    @Mock
    private EncodedValueGeometryDecoder encodedValueGeometryDecoder;


    @Test
    void lookup_ok() {
        when(booleanEncodedValueDecoder.getType()).thenReturn(Boolean.class);
        when(doubleEncodedValueDecoder.getType()).thenReturn(Double.class);

        EncodedValueDecoderRegistry encodedValueDecoderRegistry = new EncodedValueDecoderRegistry(encodedValueDistanceDecoder,
                encodedValueGeometryDecoder,
                List.of(booleanEncodedValueDecoder, doubleEncodedValueDecoder));

        assertThat(encodedValueDecoderRegistry.lookupEncodedValueDecoder(Boolean.class)).isEqualTo(Optional.of(booleanEncodedValueDecoder));
        assertThat(encodedValueDecoderRegistry.lookupEncodedValueDecoder(Double.class)).isEqualTo(Optional.of(doubleEncodedValueDecoder));
        assertThat(encodedValueDecoderRegistry.lookupEncodedValueDecoder(String.class)).isEmpty();
        assertThat(encodedValueDecoderRegistry.getEncodedValueDistanceDecoder()).isSameAs(encodedValueDistanceDecoder);
        assertThat(encodedValueDecoderRegistry.getEncodedValueGeometryDecoder()).isSameAs(encodedValueGeometryDecoder);


    }
}