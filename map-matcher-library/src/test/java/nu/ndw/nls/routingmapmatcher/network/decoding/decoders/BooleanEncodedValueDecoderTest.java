package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.BooleanEncodedValue;
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
class BooleanEncodedValueDecoderTest {

    private static final long LINK_ID = 1L;
    private static final int EDGE_ID = 1234;
    private static final String ENCODED_VALUE_NAME = "encoded_value_name";
    private static final Boolean BOOLEAN_VALUE = Boolean.TRUE;

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @InjectMocks
    private BooleanEncodedValueDecoder booleanEncodedValueDecoder;

    @Mock
    private EncodingManager encodingManager;

    @Mock
    private HashMap<Long,Integer> edgeMap;

    @Mock
    private BaseGraph baseGraph;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private BooleanEncodedValue booleanEncodedValue;

    @Test
    void retrieveValueFromNetwork_ok_forward() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getBooleanEncodedValue(ENCODED_VALUE_NAME)).thenReturn(booleanEncodedValue);

        when(edgeIteratorState.get(booleanEncodedValue)).thenReturn(BOOLEAN_VALUE);

        assertThat(booleanEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, false)).isTrue();
    }

    @Test
    void retrieveValueFromNetwork_ok_reverse() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getBooleanEncodedValue(ENCODED_VALUE_NAME)).thenReturn(booleanEncodedValue);

        when(edgeIteratorState.getReverse(booleanEncodedValue)).thenReturn(BOOLEAN_VALUE);

        assertThat(booleanEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, true)).isTrue();
    }
}