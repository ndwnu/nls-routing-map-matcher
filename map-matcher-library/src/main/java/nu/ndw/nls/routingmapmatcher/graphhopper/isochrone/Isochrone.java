package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.procedures.IntObjectProcedure;
import com.graphhopper.coll.GHIntObjectHashMap;
import com.graphhopper.routing.AbstractRoutingAlgorithm;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.ExploreType.DISTANCE;
import static nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.ExploreType.TIME;

/**
 * Based on com.graphhopper.isochrone.algorithm.Isochrone, adapted by NLS to change return type of search method.
 * @author Peter Karich
 */
public class Isochrone extends AbstractRoutingAlgorithm {

    enum ExploreType {TIME, DISTANCE}

    // TODO use same class as used in GTFS module?
    public static class IsoLabel extends SPTEntry {

        IsoLabel(int edgeId, int adjNode, double weight, long time, double distance) {
            super(edgeId, adjNode, weight);
            this.time = time;
            this.distance = distance;
        }

        public long time;
        public double distance;

        @Override
        public String toString() {
            return super.toString() + ", time:" + time + ", distance:" + distance;
        }
    }

    private IntObjectHashMap<IsoLabel> fromMap;
    private PriorityQueue<IsoLabel> fromHeap;
    private IsoLabel currEdge;
    private int visitedNodes;
    private double limit = -1;
    private ExploreType exploreType = TIME;
    private final boolean reverseFlow;

    public Isochrone(Graph g, Weighting weighting, boolean reverseFlow) {
        super(g, weighting, TraversalMode.NODE_BASED);
        fromHeap = new PriorityQueue<>(1000);
        fromMap = new GHIntObjectHashMap<>(1000);
        this.reverseFlow = reverseFlow;
    }

    @Override
    public Path calcPath(int from, int to) {
        throw new IllegalStateException("call search instead");
    }

    /**
     * Time limit in seconds
     */
    public void setTimeLimit(double limit) {
        exploreType = TIME;
        this.limit = limit * 1000;
    }

    /**
     * Distance limit in meter
     */
    public void setDistanceLimit(double limit) {
        exploreType = DISTANCE;
        this.limit = limit;
    }

    public List<IsoLabel> search(int from) {
        searchInternal(from);

        List<IsoLabel> labels = new ArrayList<>();
        fromMap.forEach((IntObjectProcedure<IsoLabel>) (nodeId, label) -> {
            // Filter out pseudo-edge that is used as a starting point
            if (label.edge != -1) {
                labels.add(label);
            }
        });
        return labels;
    }

    private void searchInternal(int from) {
        checkAlreadyRun();
        currEdge = new IsoLabel(-1, from, 0, 0, 0);
        fromMap.put(from, currEdge);
        EdgeExplorer explorer = reverseFlow ? inEdgeExplorer : outEdgeExplorer;
        while (true) {
            visitedNodes++;
            if (finished()) {
                break;
            }

            int neighborNode = currEdge.adjNode;
            EdgeIterator iter = explorer.setBaseNode(neighborNode);
            while (iter.next()) {
                if (!accept(iter, currEdge.edge)) {
                    continue;
                }
                // minor speed up
                if (currEdge.edge == iter.getEdge()) {
                    continue;
                }

                double tmpWeight = weighting.calcWeight(iter, reverseFlow, currEdge.edge) + currEdge.weight;
                if (Double.isInfinite(tmpWeight))
                    continue;

                double tmpDistance = iter.getDistance() + currEdge.distance;
                long tmpTime = weighting.calcMillis(iter, reverseFlow, currEdge.edge) + currEdge.time;
                int tmpNode = iter.getAdjNode();
                IsoLabel nEdge = fromMap.get(tmpNode);
                if (nEdge == null) {
                    nEdge = new IsoLabel(iter.getEdge(), tmpNode, tmpWeight, tmpTime, tmpDistance);
                    nEdge.parent = currEdge;
                    fromMap.put(tmpNode, nEdge);
                    fromHeap.add(nEdge);
                } else if (nEdge.weight > tmpWeight) {
                    fromHeap.remove(nEdge);
                    nEdge.edge = iter.getEdge();
                    nEdge.weight = tmpWeight;
                    nEdge.distance = tmpDistance;
                    nEdge.time = tmpTime;
                    nEdge.parent = currEdge;
                    fromHeap.add(nEdge);
                }
            }

            if (fromHeap.isEmpty()) {
                break;
            }

            currEdge = fromHeap.poll();
            if (currEdge == null) {
                throw new AssertionError("Empty edge cannot happen");
            }
        }
    }

    private double getExploreValue(IsoLabel label) {
        if (exploreType == TIME)
            return label.time;
        // if(exploreType == DISTANCE)
        return label.distance;
    }

    @Override
    protected boolean finished() {
        return getExploreValue(currEdge) >= limit;
    }

    @Override
    protected Path extractPath() {
        if (currEdge == null || !finished()) {
            return createEmptyPath();
        }
        return new Path(graph, weighting).setSPTEntry(currEdge).extract();
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
