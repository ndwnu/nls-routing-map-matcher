package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.util.EdgeIteratorState;
import java.util.HashMap;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringEncodedValueDecoderTest {

    protected static final long LINK_ID = 1L;
    protected static final int EDGE_ID = 1234;
    protected static final String ENCODED_VALUE_NAME = "encoded_value_name";
    protected static final String STRING_VALUE = "value";

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @InjectMocks
    private StringEncodedValueDecoder stringEncodedValueDecoder;

    @Mock
    private EncodingManager encodingManager;

    @Mock
    private HashMap<Long,Integer> edgeMap;

    @Mock
    private BaseGraph baseGraph;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private StringEncodedValue stringEncodedValue;

    @Test
    void retrieveValueFromNetwork_ok_forward() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getStringEncodedValue(ENCODED_VALUE_NAME)).thenReturn(stringEncodedValue);

        when(edgeIteratorState.get(stringEncodedValue)).thenReturn(STRING_VALUE);

        assertThat(stringEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, false)).isEqualTo(STRING_VALUE);
    }

    @Test
    void retrieveValueFromNetwork_ok_reverse() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getStringEncodedValue(ENCODED_VALUE_NAME)).thenReturn(stringEncodedValue);

        when(edgeIteratorState.getReverse(stringEncodedValue)).thenReturn(STRING_VALUE);

        assertThat(stringEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, true)).isEqualTo(STRING_VALUE);
    }
}