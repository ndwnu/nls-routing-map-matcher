package com.graphhopper.routing.querygraph;

import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryGraphWeightingAdapterTest {

    private static final int FIRST_VIRTUAL_EDGE_ID = 4;
    private static final int START_LINK_ID = 10;
    private static final double WEIGHT = 1D;
    private static final int NORMAL_EDGE_ID = 2;
    private static final int VIRTUAL_EDGE_ID = 5;
    @Mock
    private BaseGraph baseGraph;
    @Mock
    private Weighting weighting;
    @Mock
    private EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    @Mock
    private EncodingManager encodingManager;
    @Mock
    private EdgeIteratorState edgeState;
    @Mock
    private IntArrayList closestEdges;
    @Mock
    private IntEncodedValue encodedValue;

    private QueryGraphWeightingAdapter adapter;

    @BeforeEach
    void setup() {
        when(baseGraph.getEdges()).thenReturn(FIRST_VIRTUAL_EDGE_ID);
        adapter = new QueryGraphWeightingAdapter(baseGraph, weighting,
                closestEdges,
                edgeIteratorStateReverseExtractor,
                false, START_LINK_ID, encodingManager);
    }

    @Test
    void calcEdgeWeight_ok_virtual_edge_no_start_segment() {
        when(edgeIteratorStateReverseExtractor.hasReversed(edgeState)).thenReturn(true);
        when(edgeState.get(encodedValue)).thenReturn(0);
        when(weighting.calcEdgeWeight(edgeState, false)).thenReturn(WEIGHT);
        when(encodingManager.getIntEncodedValue(WAY_ID_KEY)).thenReturn(encodedValue);
        when(edgeState.getEdge()).thenReturn(VIRTUAL_EDGE_ID);

        double weight = adapter.calcEdgeWeight(edgeState, false);

        assertThat(weight).isEqualTo(WEIGHT);
        verify(weighting).calcEdgeWeight(edgeState, false);
    }

    @Test
    void calcEdgeWeight_ok_virtual_edge_search_direction_nok() {
        when(edgeIteratorStateReverseExtractor.hasReversed(edgeState)).thenReturn(false);
        when(weighting.calcEdgeWeight(edgeState, false)).thenReturn(WEIGHT);
        when(edgeState.getEdge()).thenReturn(VIRTUAL_EDGE_ID);
        double weight = adapter.calcEdgeWeight(edgeState, false);

        assertThat(weight).isEqualTo(WEIGHT);

        verify(weighting).calcEdgeWeight(edgeState, false);
    }

    @Test
    void calcEdgeWeight_ok_no_virtual_edge() {

        when(weighting.calcEdgeWeight(edgeState, false)).thenReturn(WEIGHT);
        when(edgeState.getEdge()).thenReturn(NORMAL_EDGE_ID);
        double weight = adapter.calcEdgeWeight(edgeState, false);
        assertThat(weight).isEqualTo(WEIGHT);

        verify(weighting).calcEdgeWeight(edgeState, false);

        verifyNoInteractions(edgeIteratorStateReverseExtractor);
    }

    @Test
    void calcEdgeWeight_ok_virtual_edge_no_access() {
        when(edgeState.getEdge()).thenReturn(VIRTUAL_EDGE_ID);
        when(edgeIteratorStateReverseExtractor.hasReversed(edgeState)).thenReturn(true);
        when(encodingManager.getIntEncodedValue(WAY_ID_KEY)).thenReturn(encodedValue);
        when(edgeState.get(encodedValue)).thenReturn(START_LINK_ID);

        double weight = adapter.calcEdgeWeight(edgeState, false);

        assertThat(weight).isEqualTo(Double.POSITIVE_INFINITY);
        verifyNoInteractions(weighting);
    }

}
