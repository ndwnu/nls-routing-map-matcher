package com.graphhopper.storage;

import com.graphhopper.routing.VirtualEdgeIteratorState;
import com.graphhopper.storage.BaseGraph.EdgeIterable;
import com.graphhopper.util.EdgeIteratorState;
import java.lang.reflect.Field;
import lombok.SneakyThrows;

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

    @SneakyThrows
    public  boolean hasReversed( EdgeIteratorState closestEdge) {
        if (closestEdge instanceof VirtualEdgeIteratorState virtualEdgeIteratorState) {
            return extractReversedFromVirtualEdge( virtualEdgeIteratorState);
        } else if (closestEdge instanceof EdgeIterable edgeIterable) {
            return edgeIterable.reverse;
        } else {
            throw new IllegalArgumentException(
                    "This method can only be called with an EdgeIterable or VirtualEdgeIteratorState"
                            + "instance of EdgeIteratorState");

        }


    }

    private static boolean extractReversedFromVirtualEdge(VirtualEdgeIteratorState closestEdge)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = closestEdge.getClass().getDeclaredField("reverse"); //NoSuchFieldException
        field.setAccessible(true);
        return (boolean) field.get(closestEdge);
    }
}
