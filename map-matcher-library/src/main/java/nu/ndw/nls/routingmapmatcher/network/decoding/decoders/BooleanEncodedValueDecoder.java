package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BooleanEncodedValueDecoder extends AbstractEncodedValueDecoder<Boolean> {

    @Override
    public Class<Boolean> getType() {
        return Boolean.class;
    }

    @Override
    protected Boolean retrieveValueFromNetwork(EncodingManager encodingManager, EdgeIteratorState edgeIteratorState,
            String encodedValueName, boolean reverse) {
        BooleanEncodedValue encodedValue = encodingManager.getBooleanEncodedValue(encodedValueName);

        if (reverse) {
            return edgeIteratorState.getReverse(encodedValue);
        } else {
            return edgeIteratorState.get(encodedValue);
        }
    }
}
