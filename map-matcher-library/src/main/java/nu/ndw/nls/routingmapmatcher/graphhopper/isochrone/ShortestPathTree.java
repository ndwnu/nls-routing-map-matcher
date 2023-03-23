package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;


import static java.util.Comparator.comparingDouble;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.GHUtility;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.function.Consumer;

/**
 * This class is a fork of the com.graphhopper.isochrone.algorithm.ShortestPathTree class. The inclusion logic is
 * different from the original class because the original current implementation differed from the previous
 * implementation in v 0.12. The previous implementation in v 0.12 included IsoLabels which had a partial limit. (ie a
 * road-segment of 100 meters which still could be travelled for 50 meters until reaching the limit where included) The
 * current implementation in the graphhopper Library did not include those partial road-segments leading to unwanted
 * results for nls requirements. This class fixes this by replacing the original check for inclusion
 * getExploreValue(label) <= limit with (this.limit - getExploreValue(isoLabel.parent)) > 0.
 */
public class ShortestPathTree extends AbstractRoutingAlgorithm {

    enum ExploreType {TIME, DISTANCE, WEIGHT}

    public static class IsoLabel {

        IsoLabel(int node, int edge, double weight, long time, double distance, IsoLabel parent) {
            this.node = node;
            this.edge = edge;
            this.weight = weight;
            this.time = time;
            this.distance = distance;
            this.parent = parent;
        }

        public boolean deleted = false;
        public int node;
        public int edge;
        public double weight;
        public long time;
        public double distance;
        public IsoLabel parent;

        @Override
        public String toString() {
            return "IsoLabel{" +
                    "node=" + node +
                    ", edge=" + edge +
                    ", weight=" + weight +
                    ", time=" + time +
                    ", distance=" + distance +
                    '}';
        }
    }

    private final IntObjectHashMap<IsoLabel> fromMap;
    private final PriorityQueue<IsoLabel> queueByWeighting;
    private int visitedNodes;
    private double limit = -1;
    private ExploreType exploreType = ExploreType.TIME;
    private final boolean reverseFlow;

    public ShortestPathTree(Graph g, Weighting weighting, boolean reverseFlow, TraversalMode traversalMode) {
        super(g, weighting, traversalMode);
        queueByWeighting = new PriorityQueue<>(1000, comparingDouble(l -> l.weight));
        fromMap = new GHIntObjectHashMap<>(1000);
        this.reverseFlow = reverseFlow;
    }

    @Override
    public Path calcPath(int from, int to) {
        throw new IllegalStateException("call search instead");
    }

    /**
     * Time limit in milliseconds
     */
    public void setTimeLimit(double limit) {
        exploreType = ExploreType.TIME;
        this.limit = limit;
    }

    /**
     * Distance limit in meter
     */
    public void setDistanceLimit(double limit) {
        exploreType = ExploreType.DISTANCE;
        this.limit = limit;
    }

    public void setWeightLimit(double limit) {
        exploreType = ExploreType.WEIGHT;
        this.limit = limit;
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
                        GHUtility.calcWeightWithTurnWeightWithAccess(weighting, iter, reverseFlow, currentLabel.edge)
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

    public Collection<IsoLabel> getIsochroneEdges() {
        // assert alreadyRun
        ArrayList<IsoLabel> result = new ArrayList<>();
        for (ObjectCursor<IsoLabel> cursor : fromMap.values()) {
            if (getExploreValue(cursor.value) > limit) {
                assert cursor.value.parent == null || getExploreValue(cursor.value.parent) <= limit;
                result.add(cursor.value);
            }
        }
        return result;
    }

    private double getExploreValue(IsoLabel label) {
        if (exploreType == ExploreType.TIME) {
            return label.time;
        }
        if (exploreType == ExploreType.WEIGHT) {
            return label.weight;
        }
        return label.distance;
    }

    private boolean isInLimit(IsoLabel isoLabel) {
        return (this.limit - getExploreValue(isoLabel.parent)) > 0;
    }

    @Override
    public String getName() {
        return "reachability";
    }

    @Override
    public int getVisitedNodes() {
        return visitedNodes;
    }
}
