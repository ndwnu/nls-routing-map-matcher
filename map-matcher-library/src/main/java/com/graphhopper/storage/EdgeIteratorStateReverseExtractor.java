package com.graphhopper.storage;

import com.graphhopper.storage.BaseGraph.EdgeIterable;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;

/**
 * The geometry direction of the edge iterator wayGeometry does not necessarily reflect the direction of a street or the
 * original encoded geometry direction. It is just the direction of the exploration of the graph. GraphHopper sometimes
 * reverses the geometry direction in the EdgeIteratorState with respect to the original geometry direction. This causes
 * problems with the bearing and fraction calculation as well as the determination whether the travelling direction is
 * reversed with respect to the original geometry direction for bidirectional road segments. To fix this an internal
 * attribute of the edge iterator state is used indicating it has done so or not. Since this attribute is package
 * private the same package structure is used.
 *
 * @see <a href="https://discuss.graphhopper.com/t/edge-direction-problem/6530">Edge direction problem</a>
 * @see <a href="https://discuss.graphhopper.com/t/understanding-edge-directions/1414">Understanding edge directions</a>
 */
public final class EdgeIteratorStateReverseExtractor {

    private EdgeIteratorStateReverseExtractor() {
    }

    public static boolean hasReversed(QueryResult q) {
        EdgeIteratorState closestEdge = q.getClosestEdge();
        if (!(closestEdge instanceof EdgeIterable edgeIterable)) {
            throw new IllegalArgumentException("This method can only be called with an EdgeIteratorState "
                    + "instance of EdgeIterable");
        }
        return edgeIterable.reverse;
    }
}
