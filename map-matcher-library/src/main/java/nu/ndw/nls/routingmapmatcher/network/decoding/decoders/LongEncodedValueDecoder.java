package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LongEncodedValueDecoder implements EncodedValueDecoder<Long> {

    private final IntegerEncodedValueDecoder integerEncodedValueDecoder;

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public Long decode(NetworkGraphHopper networkGraphHopper, long roadSectionId, String encodedValueName, boolean reverse) {
        return (long)integerEncodedValueDecoder.decode(networkGraphHopper, roadSectionId, encodedValueName, reverse);
    }

}
