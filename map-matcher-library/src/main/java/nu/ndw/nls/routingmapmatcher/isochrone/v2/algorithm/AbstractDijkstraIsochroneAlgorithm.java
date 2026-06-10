package nu.ndw.nls.routingmapmatcher.isochrone.v2.algorithm;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.GHUtility;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Consumer;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration.ExploreLimit;

@SuppressWarnings("java:S119")
public abstract class AbstractDijkstraIsochroneAlgorithm<LABEL extends IsochroneLabel> extends AbstractRoutingAlgorithm {

    private static final int INITIAL_CAPACITY = 1000;

    public static final int INVALID_EDGE = -1;

    public static final int INVALID_TRAVERSAL_ID = -1;

    private final EncodingManager encodingManager;

    private final IntObjectHashMap<LABEL> fromMap;

    private final PriorityQueue<LABEL> priorityQueue;

    private final boolean traversalInReverseFlow;

    private final Comparator<LABEL> explorePriorityComparator;

    @Getter
    private int visitedNodes;

    private final ExploreLimit<LABEL> exploreLimit;

    protected AbstractDijkstraIsochroneAlgorithm(
            Graph graph,
            EncodingManager encodingManager,
            TraversalMode traversalMode,
            boolean traversalInReverseFlow,
            Weighting weighting,
            ExploreLimit<LABEL> exploreLimit,
            Comparator<LABEL> explorePriorityComparator) {

        super(graph, weighting, traversalMode);

        this.explorePriorityComparator = explorePriorityComparator;
        priorityQueue = new PriorityQueue<>(INITIAL_CAPACITY, explorePriorityComparator);
        fromMap = new GHIntObjectHashMap<>(INITIAL_CAPACITY);

        this.encodingManager = encodingManager;
        this.traversalInReverseFlow = traversalInReverseFlow;
        this.exploreLimit = exploreLimit;
    }

    @Override
    public Path calcPath(int from, int to) {
        throw new IllegalStateException("call search instead");
    }

    @SuppressWarnings({"java:S3776", "java:S135"})
    public void search(int from, Consumer<LABEL> labelConsumer) {
        checkAlreadyRun();
        LABEL fromLabel = createNewIsoLabel(from, INVALID_EDGE, INVALID_TRAVERSAL_ID, null, 0, 0, 0, this.encodingManager);

        priorityQueue.add(fromLabel);
        if (traversalMode == TraversalMode.NODE_BASED) {
            fromMap.put(from, fromLabel);
        }
        while (!priorityQueue.isEmpty()) {
            fromLabel = priorityQueue.poll();
            if (fromLabel.isDeleted()) {
                continue;
            }
            labelConsumer.accept(fromLabel);
            fromLabel.markAsDeleted();
            visitedNodes++;

            EdgeIterator edgeIterator = edgeExplorer.setBaseNode(fromLabel.getNode());

            while (edgeIterator.next()) {
                if (!accept(edgeIterator, fromLabel.getEdge())) {
                    continue;
                }

                double toWeight = GHUtility.calcWeightWithTurnWeight(weighting, edgeIterator, traversalInReverseFlow, fromLabel.getEdge())
                                  + fromLabel.getWeight();
                if (Double.isInfinite(toWeight)) {
                    continue;
                }

                double toDistance = edgeIterator.getDistance() + fromLabel.getDistance();
                long toTime = GHUtility.calcMillisWithTurnMillis(weighting, edgeIterator, traversalInReverseFlow, fromLabel.getEdge())
                              + fromLabel.getTime();
                int toNode = edgeIterator.getAdjNode();
                int toEdge = edgeIterator.getEdge();
                int toEdgeKey = edgeIterator.getEdgeKey();
                int toTraversalId = traversalMode.createTraversalId(edgeIterator, traversalInReverseFlow);

                LABEL toLabel = fromMap.get(toTraversalId);
                if (toLabel == null) {
                    toLabel = createNewIsoLabel(toNode, toEdge, toEdgeKey, fromLabel, toTime, toDistance, toWeight, encodingManager);
                    fromMap.put(toTraversalId, toLabel);
                } else {
                    var newToLabel = createNewIsoLabel(toNode, toEdge, toEdgeKey, fromLabel, toTime, toDistance, toWeight, encodingManager);
                    if (explorePriorityComparator.compare(toLabel, newToLabel) > 0) {
                        toLabel.markAsDeleted();
                        fromMap.put(toTraversalId, newToLabel);
                        toLabel = newToLabel;
                    } else {
                        mergeEqualWeightedIsoLabels(toLabel, newToLabel);
                    }
                }

                if (isInLimit(toLabel, this.encodingManager)) {
                    priorityQueue.add(toLabel);
                } else {
                    toLabel.getParent().markAsLeafNode();
                }
            }
        }
    }

    @SuppressWarnings("java:S107")
    protected abstract LABEL createNewIsoLabel(
            int node,
            int edge,
            int edgeKey,
            LABEL parent,
            long time,
            double distance,
            double weight,
            EncodingManager encodingManager);

    protected abstract void mergeEqualWeightedIsoLabels(LABEL target, LABEL source);

    protected boolean isInLimit(LABEL isoLabel, EncodingManager encodingManager) {
        return exploreLimit.isInLimit(isoLabel, encodingManager);
    }
}
