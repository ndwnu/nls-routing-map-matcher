package nu.ndw.nls.routingmapmatcher.isochrone.v2.algorithm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
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
import nu.ndw.nls.springboot.test.logging.LoggerExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AbstractDijkstraIsochroneAlgorithmTest {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(
            "nu.ndw.nls.routingmapmatcher.isochrone.v2.algorithm.AbstractDijkstraIsochroneAlgorithm");

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

    @RegisterExtension
    LoggerExtension loggerExtension = new LoggerExtension();

    @BeforeEach
    void setUp() {
        LOGGER.setLevel(Level.DEBUG);

        when(graph.getNodeAccess()).thenReturn(nodeAccess);
        when(graph.createEdgeExplorer()).thenReturn(edgeExplorer);
        when(weighting.hasTurnCosts()).thenReturn(false);
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void calcPath_throwsIllegalStateException(TraversalMode traversalMode) {
        TestAlgorithm algorithm = createAlgorithm(traversalMode);

        assertThatThrownBy(() -> algorithm.calcPath(0, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("call search instead");
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_withNoEdgesFromRoot(TraversalMode traversalMode) {
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeIterator.next()).thenReturn(false);

        List<IsochroneLabel> visited = new ArrayList<>();
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
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

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_withOneEdgeWithinLimit(TraversalMode traversalMode) {
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
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
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

        IsochroneLabel adjacentNodeParent = adjacentNode.getParent();
        assertThat(adjacentNodeParent).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(2);
        assertThat(algorithm.getMerges()).isEmpty();

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 2(0). IsochroneLabel(node=2, edge=10, edgeKey=0, time=3600, distance=100.0, weight=10.0, leafNode=false, deleted=true)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 2       EdgeKey: 0       Distance: 100.00     Time: 3600     Weight: 10.00      Path: 1(-1) -> 2(0)");
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_withEdgeOutOfLimit(TraversalMode traversalMode) {
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeIterator.next()).thenReturn(true, false);
        when(edgeIterator.getEdge()).thenReturn(10);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(3600L);
        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(false);
        when(exploreLimit.debug(any(IsochroneLabel.class), eq(encodingManager))).thenReturn("ExploreLimit");

        List<IsochroneLabel> visited = new ArrayList<>();
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(1);

        IsochroneLabel rootNode = visited.getFirst();
        assertThat(rootNode.getNode()).isEqualTo(1);
        assertThat(rootNode.isLeafNode()).isTrue();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(1);
        assertThat(algorithm.getMerges()).isEmpty();

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 2       EdgeKey: 0       Distance: 100.00     Time: 3600     Weight: 10.00      Path: 1(-1) -> 2(0), LimitReached (not travelled), ExploreLimit");
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_stopIfEncounteredEdgeWithInfiniteWeight(TraversalMode traversalMode) {
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
        when(exploreLimit.debug(any(IsochroneLabel.class), eq(encodingManager))).thenReturn("ExploreLimit");

        List<IsochroneLabel> visited = new ArrayList<>();
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
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

        IsochroneLabel adjacentNodeParent = adjacentNode.getParent();
        assertThat(adjacentNodeParent).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(2);
        assertThat(algorithm.getMerges()).isEmpty();

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
        loggerExtension.containsLog(Level.DEBUG, "Root node: 1. LimitReached (not travelled), ExploreLimit");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 2(0). IsochroneLabel(node=2, edge=%s, edgeKey=0, time=3600, distance=100.0, weight=20.0, leafNode=false, deleted=true)"
                        .formatted(traversalMode == TraversalMode.NODE_BASED ? 20 : 10
                        ));
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 2       EdgeKey: 0       Distance: 100.00     Time: 3600     Weight: 20.00      Path: 1(-1) -> 2(0)");
    }


    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_stopIfEncounteredEdgeWithInfiniteWeight_debugModeOff(TraversalMode traversalMode) {
        LOGGER.setLevel(Level.INFO);

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
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
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

        IsochroneLabel adjacentNodeParent = adjacentNode.getParent();
        assertThat(adjacentNodeParent).isSameAs(rootNode);
        assertThat(adjacentNode.isLeafNode()).isFalse();

        assertThat(algorithm.getVisitedNodes()).isEqualTo(2);
        assertThat(algorithm.getMerges()).isEmpty();

        loggerExtension.isEmpty();
    }

    @Test
    void search_whenSecondPathToNodeIsCheaper_replacesExistingLabelAndMarksItDeleted() {
        // Graph: root(1) --[20]--> node3, root(1) --[5]--> node2 --[3]--> node3
        // Node 3 is first reached with weight 20 (direct edge from root).
        // When processing node 2 (weight 5), a cheaper path to node 3 is found (5+3=8).
        // This triggers explorePriorityComparator.compare(existing(20), new(8)) > 0 → replace.
        EdgeIterator edgeIteratorNode2 = mock(EdgeIterator.class);
        EdgeIterator edgeIteratorNode3 = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(edgeIteratorNode2);
        when(edgeExplorer.setBaseNode(3)).thenReturn(edgeIteratorNode3);

        // Two edges from root: first to node 3 (heavy, weight 20), then to node 2 (light, weight 5)
        // getEdge() and getAdjNode() are each called twice per edge: once in accept() / toNode,
        // and once in toEdge / createTraversalId(), so we need 4 return values for each.
        when(edgeIterator.next()).thenReturn(true, true, false);
        when(edgeIterator.getEdge()).thenReturn(10, 10, 11, 11);
        when(edgeIterator.getAdjNode()).thenReturn(3, 3, 2, 2);
        when(edgeIterator.getDistance()).thenReturn(200.0, 50.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(20.0, 5.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(2000L, 500L);

        // One edge from node 2 to node 3 (weight 3, cumulative 8 — cheaper than the 20 from root)
        when(edgeIteratorNode2.next()).thenReturn(true, false);
        when(edgeIteratorNode2.getEdge()).thenReturn(12);
        when(edgeIteratorNode2.getAdjNode()).thenReturn(3);
        when(edgeIteratorNode2.getDistance()).thenReturn(30.0);
        when(weighting.calcEdgeWeight(edgeIteratorNode2, false)).thenReturn(3.0);
        when(weighting.calcEdgeMillis(edgeIteratorNode2, false)).thenReturn(300L);

        when(edgeIteratorNode3.next()).thenReturn(false);

        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        TestAlgorithm algorithm = createAlgorithm(TraversalMode.NODE_BASED);
        algorithm.search(1, visited::add);

        // All three nodes are visited; the old label(3, w=20) is replaced and marked deleted so
        // it is skipped when popped from the queue, and the new cheaper label(3, w=8) is enqueued.
        assertThat(visited).hasSize(3);
        assertThat(visited.get(0).getNode()).isEqualTo(1);
        assertThat(visited.get(1).getNode()).isEqualTo(2);

        IsochroneLabel node3Label = visited.get(2);
        assertThat(node3Label.getNode()).isEqualTo(3);
        assertThat(node3Label.getWeight()).isEqualTo(8.0);
        assertThat(node3Label.getDistance()).isEqualTo(80.0);
        assertThat(node3Label.getTime()).isEqualTo(800L);

        IsochroneLabel node3LabelParent = node3Label.getParent();
        assertThat(node3LabelParent).isSameAs(visited.get(1));
        assertThat(algorithm.getVisitedNodes()).isEqualTo(3);

        assertThat(algorithm.getMerges()).isEmpty();

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 3(0). IsochroneLabel(node=3, edge=10, edgeKey=0, time=2000, distance=200.0, weight=20.0, leafNode=false, deleted=true)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 2(0). IsochroneLabel(node=2, edge=11, edgeKey=0, time=500, distance=50.0, weight=5.0, leafNode=false, deleted=true)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 2       EdgeKey: 0       Distance: 50.00      Time: 500      Weight: 5.00       Path: 1(-1) -> 2(0)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 2(0) -> 3(0). IsochroneLabel(node=3, edge=12, edgeKey=0, time=800, distance=80.0, weight=8.0, leafNode=false, deleted=true)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 3       EdgeKey: 0       Distance: 80.00      Time: 800      Weight: 8.00       Path: 1(-1) -> 2(0) -> 3(0)");
    }

    @ParameterizedTest
    @EnumSource(TraversalMode.class)
    void search_mergeEqualWeightedPaths(TraversalMode traversalMode) {
        // node 0 → (10) → node 1 → (20) → node 0
        // When processing node 1 (weight=10): fromLabel.weight(10) > root.weight(0) → replace
        EdgeIterator edgeIteratorNode2 = mock(EdgeIterator.class);
        when(edgeExplorer.setBaseNode(1)).thenReturn(edgeIterator);
        when(edgeExplorer.setBaseNode(2)).thenReturn(edgeIteratorNode2);

        when(edgeIterator.next()).thenReturn(true, false);
        when(edgeIterator.getEdge()).thenReturn(10);
        when(edgeIterator.getAdjNode()).thenReturn(2);
        when(edgeIterator.getDistance()).thenReturn(100.0);
        when(weighting.calcEdgeWeight(edgeIterator, false)).thenReturn(10.0, 11.0);
        when(weighting.calcEdgeMillis(edgeIterator, false)).thenReturn(1000L);

        when(edgeIteratorNode2.next()).thenReturn(true, false);
        when(edgeIteratorNode2.getEdge()).thenReturn(20);
        when(edgeIteratorNode2.getAdjNode()).thenReturn(1);
        when(edgeIteratorNode2.getDistance()).thenReturn(50.0);
        when(weighting.calcEdgeWeight(edgeIteratorNode2, false)).thenReturn(5.0);
        when(weighting.calcEdgeMillis(edgeIteratorNode2, false)).thenReturn(500L);

        when(exploreLimit.isInLimit(any(IsochroneLabel.class), eq(encodingManager))).thenReturn(true);

        List<IsochroneLabel> visited = new ArrayList<>();
        TestAlgorithm algorithm = createAlgorithm(traversalMode);
        algorithm.search(1, visited::add);

        assertThat(visited).hasSize(2);
        assertThat(visited.get(0).getNode()).isEqualTo(1);
        assertThat(visited.get(1).getNode()).isEqualTo(2);
        assertThat(visited.get(1).getWeight()).isEqualTo(10);

        loggerExtension.containsLog(Level.DEBUG, "Root node: 1");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Adding new label: 1(-1) -> 2(0). IsochroneLabel(node=2, edge=10, edgeKey=0, time=1000, distance=100.0, weight=10.0, leafNode=false, deleted=true)");
        loggerExtension.containsLog(
                Level.DEBUG,
                "Node 2       EdgeKey: 0       Distance: 100.00     Time: 1000     Weight: 10.00      Path: 1(-1) -> 2(0)");
    }

    private TestAlgorithm createAlgorithm(TraversalMode traversalMode) {
        return new TestAlgorithm(
                graph,
                encodingManager,
                traversalMode,
                false,
                weighting,
                exploreLimit,
                Comparator.comparingDouble(IsochroneLabel::getWeight));
    }

    @Getter
    static class TestAlgorithm extends AbstractDijkstraIsochroneAlgorithm<IsochroneLabel> {

        private final List<String> merges = new ArrayList<>();

        private final List<IsochroneLabel> createdLabels = new ArrayList<>();

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
                int node,
                int edge,
                int edgeKey,
                IsochroneLabel parent,
                long time,
                double distance,
                double weight,
                EncodingManager encodingManager) {
            IsochroneLabel label = new IsochroneLabel(node, edge, edgeKey, parent, time, distance, weight);
            createdLabels.add(label);
            return label;
        }

        @Override
        protected void mergeEqualWeightedIsoLabels(IsochroneLabel target, IsochroneLabel source) {
            merges.add(target.getNode() + "<-" + source.getNode());
        }
    }
}
