package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedBooleanValueFactory implements EncodedValueFactory<Boolean> {

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    public <T extends Link> EncodedValue encode(EncodedValueDto<T, Boolean> encodedValueDto) {
        return new SimpleBooleanEncodedValue(encodedValueDto.key(), encodedValueDto.isDirectional());
    }

}
