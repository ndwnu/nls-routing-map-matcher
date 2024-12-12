package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IntegerEncodedValueDecoder extends AbstractEncodedValueDecoder<Integer> {

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    protected Integer retrieveValueFromNetwork(EncodingManager encodingManager, EdgeIteratorState edgeIteratorState,
            String encodedValueName, boolean reverse) {

        IntEncodedValue intEncodedValue = encodingManager.getIntEncodedValue(encodedValueName);

        if (reverse) {
            return edgeIteratorState.getReverse(intEncodedValue);
        } else {
            return edgeIteratorState.get(intEncodedValue);
        }
    }
}
