package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;

public interface EncodedValueDecoder<T> {

    Class<T> getType();

    T decode(NetworkGraphHopper networkGraphHopper, long roadSectionId, String encodedValueName,
            boolean reverse);
}
