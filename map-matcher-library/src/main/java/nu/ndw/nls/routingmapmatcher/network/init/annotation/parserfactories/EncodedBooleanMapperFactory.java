package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedBooleanMapper;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedBooleanMapperFactory implements EncodedMapperFactory<Boolean> {
    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public <U extends Link> AbstractEncodedMapper<U, Boolean> create(EncodedValueLookup lookup,
            EncodedValueDto<U, Boolean> encodedValueDto) {
        return new EncodedBooleanMapper<>(lookup, encodedValueDto);
    }

}
