package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm;


import static java.util.Comparator.comparingDouble;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.GHUtility;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * This class is a fork of the com.graphhopper.isochrone.algorithm.IsochroneByTimeDistanceAndWeight class. The inclusion logic is
 * different from the original class because the original current implementation differed from the previous
 * implementation in v 0.12. The previous implementation in v 0.12 included IsoLabels which had a partial limit (ie a
 * road-segment of 100 meters which still could be travelled for 50 meters until reaching the limit was included). The
 * current implementation in the graphhopper Library did not include those partial road-segments leading to unwanted
 * results for nls requirements. This class fixes this by replacing the original check for inclusion
 * getExploreValue(label) <= limit with (this.limit - getExploreValue(isoLabel.parent)) > 0.
 */
public abstract class AbstractShortestPathTree extends AbstractRoutingAlgorithm {




    private final IntObjectHashMap<IsoLabel> fromMap;
    private final PriorityQueue<IsoLabel> queueByWeighting;
    private int visitedNodes;

    private final boolean reverseFlow;

    public AbstractShortestPathTree(Graph g, Weighting weighting, boolean reverseFlow, TraversalMode traversalMode) {
        super(g, weighting, traversalMode);
        queueByWeighting = new PriorityQueue<>(1000, comparingDouble(l -> l.weight));
        fromMap = new GHIntObjectHashMap<>(1000);
        this.reverseFlow = reverseFlow;
    }

    @Override
    public Path calcPath(int from, int to) {
        throw new IllegalStateException("call search instead");
    }

    public void search(int from, final Consumer<IsoLabel> consumer) {
        checkAlreadyRun();
        IsoLabel currentLabel = new IsoLabel(from, -1, 0, 0, 0, null);
        queueByWeighting.add(currentLabel);
        if (traversalMode == TraversalMode.NODE_BASED) {
            fromMap.put(from, currentLabel);
        }
        while (!queueByWeighting.isEmpty()) {
            currentLabel = queueByWeighting.poll();
            if (currentLabel.deleted) {
                continue;
            }
            consumer.accept(currentLabel);
            currentLabel.deleted = true;
            visitedNodes++;

            EdgeIterator iter = edgeExplorer.setBaseNode(currentLabel.node);
            while (iter.next()) {
                if (!accept(iter, currentLabel.edge)) {
                    continue;
                }

                double nextWeight =
                        GHUtility.calcWeightWithTurnWeight(weighting, iter, reverseFlow, currentLabel.edge)
                                + currentLabel.weight;
                if (Double.isInfinite(nextWeight)) {
                    continue;
                }

                double nextDistance = iter.getDistance() + currentLabel.distance;
                long nextTime = GHUtility.calcMillisWithTurnMillis(weighting, iter, reverseFlow, currentLabel.edge)
                        + currentLabel.time;
                int nextTraversalId = traversalMode.createTraversalId(iter, reverseFlow);
                IsoLabel label = fromMap.get(nextTraversalId);
                if (label == null) {
                    label = new IsoLabel(iter.getAdjNode(), iter.getEdge(), nextWeight, nextTime, nextDistance,
                            currentLabel);
                    fromMap.put(nextTraversalId, label);
                    if (isInLimit(label)) {
                        queueByWeighting.add(label);
                    }
                } else if (label.weight > nextWeight) {
                    label.deleted = true;
                    label = new IsoLabel(iter.getAdjNode(), iter.getEdge(), nextWeight, nextTime, nextDistance,
                            currentLabel);
                    fromMap.put(nextTraversalId, label);
                    if (isInLimit(label)) {
                        queueByWeighting.add(label);
                    }
                }
            }
        }
    }


    protected abstract boolean isInLimit(IsoLabel isoLabel);

    @Override
    public String getName() {
        return "reachability";
    }

    @Override
    public int getVisitedNodes() {
        return visitedNodes;
    }
}




