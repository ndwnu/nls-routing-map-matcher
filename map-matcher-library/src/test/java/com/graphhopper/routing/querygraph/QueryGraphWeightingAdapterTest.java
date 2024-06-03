package com.graphhopper.routing.querygraph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.DOUBLE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryGraphWeightingAdapterTest {

    public static final int FIRST_VIRTUAL_NODE_ID = 3;
    public static final int FIRST_VIRTUAL_EDGE_ID = 4;
    public static final double WEIGHT = 1D;
    @Mock
    private Weighting weighting;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private EdgeIteratorState edgeState;
    @Mock
    private IntArrayList closestEdges;

    private QueryGraphWeightingAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new QueryGraphWeightingAdapter(weighting, FIRST_VIRTUAL_NODE_ID,
                FIRST_VIRTUAL_EDGE_ID,
                closestEdges,
                edgeIteratorStateReverseExtractor,
                false);
    }

    @Test
    void calcEdgeWeight_ok_no_virtual_edge() {
        when(weighting.calcEdgeWeight(edgeState, false))
                .thenReturn(WEIGHT);
        when(edgeState.getEdge()).thenReturn(2);
        double weight = adapter.calcEdgeWeight(edgeState, false);
        assertThat(weight).isEqualTo(WEIGHT);
        verify(weighting).calcEdgeWeight(edgeState, false);
        verifyNoInteractions(edgeIteratorStateReverseExtractor);
    }

    @Test
    void calcEdgeWeight_ok_virtual_edge_no_access() {
        when(edgeState.getEdge()).thenReturn(5);
        when(edgeIteratorStateReverseExtractor.hasReversed(edgeState)).thenReturn(true);
        double weight = adapter.calcEdgeWeight(edgeState, false);
        assertThat(weight).isEqualTo(Double.POSITIVE_INFINITY);
        verifyNoInteractions(weighting);
    }
}
