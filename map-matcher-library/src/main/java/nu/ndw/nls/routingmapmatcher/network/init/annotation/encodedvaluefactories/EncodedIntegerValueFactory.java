package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedIntegerValueFactory implements EncodedValueFactory<Integer> {

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public <T extends Link> EncodedValue encode(EncodedValueDto<T, Integer> encodedValueDto) {
        return new IntEncodedValueImpl(encodedValueDto.key(), encodedValueDto.bits(), encodedValueDto.isDirectional());
    }

}
