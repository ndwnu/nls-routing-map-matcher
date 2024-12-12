package nu.ndw.nls.routingmapmatcher.network.init.annotation.parserfactories;

import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.AbstractEncodedMapper;
import nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers.EncodedDoubleMapper;
import org.springframework.stereotype.Component;

@Component
public class EncodedDoubleMapperFactory implements EncodedMapperFactory<Double> {
    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public <U extends NetworkEncoded> AbstractEncodedMapper<U, Double> create(EncodedValueLookup lookup,
            EncodedValueDto<U, Double> encodedValueDto) {
        return new EncodedDoubleMapper<>(lookup, encodedValueDto);
    }

}
