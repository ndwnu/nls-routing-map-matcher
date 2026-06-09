package nu.ndw.nls.routingmapmatcher.isochrone.v2.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration.ExploreLimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractDijkstraIsochroneAlgorithmTest {

    @Mock
    private Graph graph;

    @Mock
    private NodeAccess nodeAccess;

    @Mock
    private EdgeExplorer edgeExplorer;

    @Mock
    private EdgeIterator edgeIterator;

    @Mock
    private Weighting weighting;

    @Mock
    private EncodingManager encodingManager;

    @Mock
    private ExploreLimit<IsochroneLabel> exploreLimit;

    private TestAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        when(graph.getNodeAccess()).thenReturn(nodeAccess);
        when(graph.createEdgeExplorer()).thenReturn(edgeExplorer);
        when(weighting.hasTurnCosts()).thenReturn(false);

        algorithm = new TestAlgorithm(
                graph,
                encodingManager,
                TraversalMode.NODE_BASED,
                false,
                weighting,
                exploreLimit,
                Comparator.comparingDouble(IsochroneLabel::getWeight));
    }

    @Test
    void calcPath_throwsIllegalStateException() {
        assertThatThrownBy(() -> algorithm.calcPath(0, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("call search instead");
    }

    @Test
    void search_withNoEdgesFromRoot() {
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeIterator.next()).thenReturn(false);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(1);
        IsochroneLabel root = visited.getFirst();
        assertThat(root.getNode()).isEqualTo(1);
        assertThat(root.isRoot()).isTrue();
        assertThat(root.getWeight()).isEqualTo(0.0);
        assertThat(root.getTime()).isZero();
        assertThat(root.getDistance()).isEqualTo(0.0);

        assertThat(algorithm.getVisitedNodes()).isEqualTo(1);
        assertThat(algorithm.getMerges()).isEmpty();
    }

    @Test
    void search_withOneEdgeWithinLimit() {
        EdgeIterator emptyIterator = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(emptyIterator);
        when(edgeIterator.next()).thenReturn(true, false);
        when(edgeIterator.getEdge()).thenReturn(10);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(3600L);
        when(emptyIterator.next()).thenReturn(false);
        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(2);
        IsochroneLabel rootNode = visited.getFirst();
        assertThat(rootNode.getNode()).isEqualTo(1);
        assertThat(rootNode.isLeafNode()).isFalse();

        IsochroneLabel adjacentNode = visited.get(1);
        assertThat(adjacentNode.getNode()).isEqualTo(2);
        assertThat(adjacentNode.getWeight()).isEqualTo(10.0);
        assertThat(adjacentNode.getDistance()).isEqualTo(100.0);
        assertThat(adjacentNode.getTime()).isEqualTo(3600L);
        assertThat(adjacentNode.getParent()).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(2);
        assertThat(algorithm.getMerges()).isEmpty();
    }

    @Test
    void search_withEdgeOutOfLimit() {
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeIterator.next()).thenReturn(true, false);
        when(edgeIterator.getEdge()).thenReturn(10);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(3600L);
        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(false);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(1);

        IsochroneLabel rootNode = visited.getFirst();
        assertThat(rootNode.getNode()).isEqualTo(1);
        assertThat(rootNode.isLeafNode()).isTrue();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(1);
        assertThat(algorithm.getMerges()).isEmpty();
    }

    @Test
    void search_stopIfEncounteredEdgeWithInfiniteWeight() {
        EdgeIterator emptyIterator = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(emptyIterator);
        when(edgeIterator.next()).thenReturn(true, true, false);
        when(edgeIterator.getEdge()).thenReturn(10, 20);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(Double.POSITIVE_INFINITY, 20.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(3600L);
        when(emptyIterator.next()).thenReturn(false);
        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(2);
        IsochroneLabel rootNode = visited.getFirst();
        assertThat(rootNode.getNode()).isEqualTo(1);
        assertThat(rootNode.isLeafNode()).isFalse();

        IsochroneLabel adjacentNode = visited.get(1);
        assertThat(adjacentNode.getNode()).isEqualTo(2);
        assertThat(adjacentNode.getWeight()).isEqualTo(20.0);
        assertThat(adjacentNode.getDistance()).isEqualTo(100.0);
        assertThat(adjacentNode.getTime()).isEqualTo(3600L);
        assertThat(adjacentNode.getParent()).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(2);
        assertThat(algorithm.getMerges()).isEmpty();
    }

    @Test
    void search_withTwoEdgesToSameNode() {
        EdgeIterator emptyIterator = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(emptyIterator);
        when(edgeIterator.next()).thenReturn(true, true, false);
        when(edgeIterator.getEdge()).thenReturn(10, 20);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0, 50.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0, 20.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(1000L, 500L);
        when(emptyIterator.next()).thenReturn(false);
        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(algorithm.getMerges()).containsExactly("2<-1");
        assertThat(visited).hasSize(2);
        IsochroneLabel rootNode = visited.getFirst();
        assertThat(rootNode.getNode()).isEqualTo(1);
        assertThat(rootNode.isLeafNode()).isFalse();

        IsochroneLabel adjacentNode = visited.get(1);
        assertThat(adjacentNode.getNode()).isEqualTo(2);
        assertThat(adjacentNode.getWeight()).isEqualTo(10.0);
        assertThat(adjacentNode.getDistance()).isEqualTo(100);
        assertThat(adjacentNode.getTime()).isEqualTo(1000L);
        assertThat(adjacentNode.getParent()).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();
    }

    @Test
    void search_whenFromLabelWeightExceedsExistingLabelWeight_replaceOutweighedLabelWithNewLabel() {
        // node 0 → (10) → node 1 → (20) → node 0
        // When processing node 1 (weight=10): fromLabel.weight(10) > root.weight(0) → replace
        EdgeIterator edgeIteratorNode2 = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(edgeIteratorNode2);

        when(edgeIterator.next()).thenReturn(true, false);
        when(edgeIterator.getEdge()).thenReturn(10);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(1000L);

        when(edgeIteratorNode2.next()).thenReturn(true, false);
        when(edgeIteratorNode2.getEdge()).thenReturn(20);
        when(edgeIteratorNode2.getAdjNode()).thenReturn(1);
        when(edgeIteratorNode2.getDistance()).thenReturn(50.0);
        when(weighting.calcEdgeWeight(edgeIteratorNode2, false)).thenReturn(5.0);
        when(weighting.calcEdgeMillis(edgeIteratorNode2, false)).thenReturn(500L);

        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);
        when(exploreLimit.isInLimit(isNull(), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(2);
        assertThat(visited.get(0).getNode()).isEqualTo(1);
        assertThat(visited.get(1).getNode()).isEqualTo(2);
        assertThat(visited.get(1).getWeight()).isEqualTo(10);
    }

    @Getter
    static class TestAlgorithm extends AbstractDijkstraIsochroneAlgorithm<IsochroneLabel> {

        private final List<String> merges = new ArrayList<>();

        TestAlgorithm(
                Graph graph,
                EncodingManager encodingManager,
                TraversalMode traversalMode,
                boolean reverseFlow,
                Weighting weighting,
                ExploreLimit<IsochroneLabel> exploreLimit,
                Comparator<IsochroneLabel> explorePriorityComparator) {
            super(graph, encodingManager, traversalMode, reverseFlow, weighting, exploreLimit, explorePriorityComparator);
        }

        @Override
        protected IsochroneLabel createNewIsoLabel(
                int node, int edge, int edgeKey, IsochroneLabel parent,
                long time, double distance, double weight, EncodingManager em) {
            return new IsochroneLabel(node, edge, edgeKey, weight, time, distance, parent);
        }

        @Override
        protected void mergeEqualWeightedIsoLabels(IsochroneLabel target, IsochroneLabel source) {
            merges.add(target.getNode() + "<-" + source.getNode());
        }
    }
}
