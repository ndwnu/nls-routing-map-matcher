package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.ev.DecimalEncodedValue;
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
class DoubleEncodedValueDecoderTest {

    private static final long LINK_ID = 1L;
    private static final int EDGE_ID = 1234;
    private static final String ENCODED_VALUE_NAME = "encoded_value_name";
    private static final double DOUBLE_VALUE = 123D;

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @InjectMocks
    private DoubleEncodedValueDecoder doubleEncodedValueDecoder;

    @Mock
    private EncodingManager encodingManager;

    @Mock
    private HashMap<Long,Integer> edgeMap;

    @Mock
    private BaseGraph baseGraph;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Mock
    private DecimalEncodedValue decimalEncodedValue;

    @Test
    void retrieveValueFromNetwork_ok_forward() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getDecimalEncodedValue(ENCODED_VALUE_NAME)).thenReturn(decimalEncodedValue);

        when(edgeIteratorState.get(decimalEncodedValue)).thenReturn(DOUBLE_VALUE);

        assertThat(doubleEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, false)).isEqualTo(DOUBLE_VALUE);
    }

    @Test
    void retrieveValueFromNetwork_ok_reverse() {
        when(networkGraphHopper.getEncodingManager()).thenReturn(encodingManager);

        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(edgeMap.get(LINK_ID)).thenReturn(EDGE_ID);

        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(EDGE_ID)).thenReturn(edgeIteratorState);

        when(encodingManager.getDecimalEncodedValue(ENCODED_VALUE_NAME)).thenReturn(decimalEncodedValue);

        when(edgeIteratorState.getReverse(decimalEncodedValue)).thenReturn(DOUBLE_VALUE);

        assertThat(doubleEncodedValueDecoder.decode(networkGraphHopper, LINK_ID, ENCODED_VALUE_NAME, true)).isEqualTo(DOUBLE_VALUE);
    }
    
}