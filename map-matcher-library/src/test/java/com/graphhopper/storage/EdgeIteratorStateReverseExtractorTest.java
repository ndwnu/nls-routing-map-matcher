package com.graphhopper.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

import com.graphhopper.storage.BaseGraph.EdgeIteratorStateImpl;
import com.graphhopper.util.EdgeIteratorState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EdgeIteratorStateReverseExtractorTest {

    @Mock
    private EdgeIteratorStateImpl edgeIteratorStateImpl;

    @Mock
    private EdgeIteratorState otherEdgeIteratorState;

    @Test
    void getEdgeIteratorStateImpl_ok() {

       assertThat(EdgeIteratorStateReverseExtractor.getEdgeIteratorStateImpl(edgeIteratorStateImpl)).isEqualTo(edgeIteratorStateImpl);
    }

    @Test
    void getEdgeIteratorStateImpl_invalidObjectType() {

        when(otherEdgeIteratorState.toString()).thenReturn("object_toStringValue");

        assertThat(catchThrowable(
                () -> EdgeIteratorStateReverseExtractor.getEdgeIteratorStateImpl(otherEdgeIteratorState)))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(
                        "This method can only be called with an EdgeIteratorStateImpl instance of EdgeIteratorState object_toStringValue");
    }
}