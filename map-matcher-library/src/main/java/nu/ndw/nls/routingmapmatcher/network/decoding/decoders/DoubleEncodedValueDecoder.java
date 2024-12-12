package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DoubleEncodedValueDecoder extends AbstractEncodedValueDecoder<Double> {

    @Override
    public Class<Double> getType() {
        return Double.class;
    }

    @Override
    protected Double retrieveValueFromNetwork(EncodingManager encodingManager, EdgeIteratorState edgeIteratorState,
            String encodedValueName, boolean reverse) {
        DecimalEncodedValue decimalEncodedValue = encodingManager.getDecimalEncodedValue(encodedValueName);
        if (reverse) {
            return edgeIteratorState.getReverse(decimalEncodedValue);
        } else {
            return edgeIteratorState.get(decimalEncodedValue);
        }
    }
}
