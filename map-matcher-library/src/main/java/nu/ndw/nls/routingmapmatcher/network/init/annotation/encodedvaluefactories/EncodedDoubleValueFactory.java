package nu.ndw.nls.routingmapmatcher.network.init.annotation.encodedvaluefactories;

import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import org.springframework.stereotype.Component;

@Component
public class EncodedDoubleValueFactory implements EncodedValueFactory<Double> {
    private static final double DECIMAL_FACTOR = 0.1;


    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    public <T extends Link> EncodedValue encode(EncodedValueDto<T, Double> encodedValueDto) {
        return new DecimalEncodedValueImpl(encodedValueDto.key(), encodedValueDto.bits(), 0.0,
                DECIMAL_FACTOR, false, encodedValueDto.isDirectional(), true);
    }
}
