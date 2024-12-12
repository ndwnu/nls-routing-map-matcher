package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

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
class EncodedValueDistanceDecoderTest {

    private static final long ROAD_SECTION_ID = 123;
    private static final int INTERNAL_EDGE_ID = 678;
    private static final double DISTANCE = 100D;

    @InjectMocks
    private EncodedValueDistanceDecoder encodedValueDistanceDecoder;

    @Mock
    private NetworkGraphHopper networkGraphHopper;

    @Mock
    private BaseGraph baseGraph;

    @Mock
    private EdgeIteratorState edgeIteratorState;

    @Test
    void decode_ok() {
        HashMap<Long, Integer> edgeMap = new HashMap<>();
        edgeMap.put(ROAD_SECTION_ID, INTERNAL_EDGE_ID);
        when(networkGraphHopper.getEdgeMap()).thenReturn(edgeMap);
        when(networkGraphHopper.getBaseGraph()).thenReturn(baseGraph);
        when(baseGraph.getEdgeIteratorStateForKey(INTERNAL_EDGE_ID)).thenReturn(edgeIteratorState);
        when(edgeIteratorState.getDistance()).thenReturn(DISTANCE);

        assertThat(encodedValueDistanceDecoder.decode(networkGraphHopper, ROAD_SECTION_ID)).isEqualTo(DISTANCE);
    }

}