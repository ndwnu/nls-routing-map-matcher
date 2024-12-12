package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

/**
 * This long encoder is only used for "way_id", our {@link Link} id and is not truly encoded as Long but as an integer
 */
@Component
public class EncodedLongValueFactory implements EncodedValueFactory<Long> {

    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public <T extends NetworkEncoded> EncodedValue encode(EncodedValueDto<T, Long> encodedValueDto) {
        return new IntEncodedValueImpl(encodedValueDto.key(), encodedValueDto.bits(), encodedValueDto.isDirectional());
    }

}
