package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StringEncodedValueDecoder extends AbstractEncodedValueDecoder<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    protected String retrieveValueFromNetwork(EncodingManager encodingManager, EdgeIteratorState edgeIteratorState, String encodedValueName,
            boolean reverse) {
        StringEncodedValue stringEncodedValue = encodingManager.getStringEncodedValue(encodedValueName);

        if (reverse) {
            return edgeIteratorState.getReverse(stringEncodedValue);
        } else {
            return edgeIteratorState.get(stringEncodedValue);
        }
    }
}
