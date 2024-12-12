package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedIntegerMapper;
import org.springframework.stereotype.Component;

@Component
public class EncodedIntegerMapperFactory implements EncodedMapperFactory<Integer> {
    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public <U extends NetworkEncoded> AbstractEncodedMapper<U, Integer> create(EncodedValueLookup lookup,
            EncodedValueDto<U, Integer> encodedValueDto) {
        return new EncodedIntegerMapper<>(lookup, encodedValueDto);
    }

}
