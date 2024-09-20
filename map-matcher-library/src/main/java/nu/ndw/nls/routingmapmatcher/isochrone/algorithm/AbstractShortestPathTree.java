package nu.ndw.nls.routingmapmatcher.isochrone.algorithm;


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
 * This class is a fork of the com.graphhopper.isochrone.algorithm. ShortestPathTree class. The inclusion logic is moved
 * to an abstract template method to allow more limiting conditions in subclasses and still reusing the tree traversal
 * algorithm.
 */
public abstract class AbstractShortestPathTree extends AbstractRoutingAlgorithm {

    private static final int INITIAL_CAPACITY = 1000;
    private final IntObjectHashMap<IsoLabel> fromMap;
    private final PriorityQueue<IsoLabel> queueByWeighting;
    private int visitedNodes;

    private final boolean upstream;
    private final boolean startingDirectionReversed;

    public AbstractShortestPathTree(Graph g, Weighting weighting, boolean upstream, boolean startingDirectionReversed, TraversalMode traversalMode) {
        super(g, weighting, traversalMode);
        queueByWeighting = new PriorityQueue<>(INITIAL_CAPACITY, comparingDouble(IsoLabel::getWeight));
        fromMap = new GHIntObjectHashMap<>(INITIAL_CAPACITY);
        this.upstream = upstream;
        this.startingDirectionReversed = startingDirectionReversed;
    }

    @Override
    public Path calcPath(int from, int to) {
        throw new IllegalStateException("call search instead");
    }

    public void search(int from, Consumer<IsoLabel> consumer) {
        checkAlreadyRun();
        IsoLabel currentLabel = new IsoLabel(from, -1, 0, 0, 0, null);
        queueByWeighting.add(currentLabel);
        if (traversalMode == TraversalMode.NODE_BASED) {
            fromMap.put(from, currentLabel);
        }
        while (!queueByWeighting.isEmpty()) {
            currentLabel = queueByWeighting.poll();
            if (currentLabel.isDeleted()) {
                continue;
            }
            consumer.accept(currentLabel);
            currentLabel.setDeleted(true);
            visitedNodes++;

            EdgeIterator iter = edgeExplorer.setBaseNode(currentLabel.getNode());
            while (iter.next()) {
                if (!accept(iter, currentLabel.getEdge())) {
                    continue;
                }

                double nextWeight =
                        GHUtility.calcWeightWithTurnWeight(weighting, iter, upstream != startingDirectionReversed, currentLabel.getEdge())
                                + currentLabel.getWeight();
                if (Double.isInfinite(nextWeight)) {
                    continue;
                }

                double nextDistance = iter.getDistance() + currentLabel.getDistance();
                long nextTime = GHUtility.calcMillisWithTurnMillis(weighting, iter, upstream != startingDirectionReversed, currentLabel.getEdge())
                        + currentLabel.getTime();
                int nextTraversalId = traversalMode.createTraversalId(iter, upstream != startingDirectionReversed);
                IsoLabel label = fromMap.get(nextTraversalId);
                if (label == null) {
                    label = new IsoLabel(iter.getAdjNode(), iter.getEdge(), nextWeight, nextTime, nextDistance,
                            currentLabel);
                    fromMap.put(nextTraversalId, label);
                    if (isInLimit(label)) {
                        queueByWeighting.add(label);
                    }
                } else if (label.getWeight() > nextWeight) {
                    label.setDeleted(true);
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
