package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedStringValueFactory implements EncodedValueFactory<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public <T extends Link> EncodedValue encode(EncodedValueDto<T, String> encodedValueDto) {
        return new StringEncodedValue(encodedValueDto.key(), 1, encodedValueDto.isDirectional());
    }

}
