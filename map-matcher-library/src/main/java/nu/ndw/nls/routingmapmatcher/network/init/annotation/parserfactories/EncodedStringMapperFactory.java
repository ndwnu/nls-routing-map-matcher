package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedStringValueMapper;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedStringMapperFactory implements EncodedMapperFactory<String> {
    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public <U extends Link> AbstractEncodedMapper<U, String> create(EncodedValueLookup lookup,
            EncodedValueDto<U, String> encodedValueDto) {
        return new EncodedStringValueMapper<>(lookup, encodedValueDto);
    }

}
