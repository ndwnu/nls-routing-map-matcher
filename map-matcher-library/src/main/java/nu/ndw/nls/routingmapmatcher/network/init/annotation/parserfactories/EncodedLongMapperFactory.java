package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedLongMapper;
import org.springframework.stereotype.Component;

@Component
public class EncodedLongMapperFactory implements EncodedMapperFactory<Long> {
    @Override
    public Class<Long> getType() {
        return Long.class;
    }

    @Override
    public <U extends NetworkEncoded> AbstractEncodedMapper<U, Long> create(EncodedValueLookup lookup,
            EncodedValueDto<U, Long> encodedValueDto) {
        return new EncodedLongMapper<>(lookup, encodedValueDto);
    }

}
