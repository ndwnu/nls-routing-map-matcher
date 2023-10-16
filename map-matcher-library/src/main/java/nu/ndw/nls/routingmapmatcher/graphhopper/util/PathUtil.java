package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.WAY_ID;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;

public final class PathUtil {

    private static final EdgeIteratorStateReverseExtractor EDGE_ITERATOR_STATE_REVERSE_EXTRACTOR =
            new EdgeIteratorStateReverseExtractor();

    private PathUtil() {
        // Util class
    }

    public static List<MatchedLink> determineMatchedLinks(EncodingManager encodingManager,
            Collection<EdgeIteratorState> edges) {
        List<MatchedLink> matchedLinks = new ArrayList<>();
        for (EdgeIteratorState edge : edges) {
            int matchedLinkId = edge.get(encodingManager.getIntEncodedValue(WAY_ID.getKey()));
            if (matchedLinks.isEmpty() || matchedLinks.get(matchedLinks.size() - 1).getLinkId() != matchedLinkId) {
                matchedLinks.add(MatchedLink.builder()
                        .linkId(matchedLinkId)
                        .reversed(EDGE_ITERATOR_STATE_REVERSE_EXTRACTOR.hasReversed(edge))
                        .build());
            }
        }
        return matchedLinks;
    }

    /**
     * This method determines the direction on which one can travel over a path. We always need to validate the travel
     * direction because of the way how graphhopper creates node indexes and always wants to store edges with the lower
     * node index followed by the higher node index:
     * https://github.com/graphhopper/graphhopper/blob/master/docs/core/technical.md
     * <p>
     * Even though we supply nodes in driving direction (base node to adjacent node), graphhopper sometimes decides to
     * return results in reverse direction.
     *
     * @param queryResult
     * @param encodingManager
     * @return travel direction on this specific edge
     */
    public static EdgeIteratorTravelDirection determineEdgeDirection(Snap queryResult,
            EncodingManager encodingManager) {
        EdgeIteratorState edge = queryResult.getClosestEdge();
        boolean edgeCanBeTraveledFromBaseToAdjacent = edge.get(
                encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR)));
        boolean edgeCanBeTraveledFromAdjacentToBase = edge.getReverse(
                encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR)));
        if (edgeCanBeTraveledFromAdjacentToBase && edgeCanBeTraveledFromBaseToAdjacent) {
            return EdgeIteratorTravelDirection.BOTH_DIRECTIONS;
        } else if (edgeCanBeTraveledFromAdjacentToBase) {
            return EdgeIteratorTravelDirection.REVERSED;
        } else if (edgeCanBeTraveledFromBaseToAdjacent) {
            return EdgeIteratorTravelDirection.FORWARD;
        } else {
            throw new IllegalStateException("Edge has no travel direction");
        }
    }

    public static double determineStartLinkFraction(EdgeIteratorState firstEdge, QueryGraph queryGraph) {
        if (queryGraph.isVirtualNode(firstEdge.getBaseNode())) {
            EdgeIteratorState originalEdge = findOriginalEdge(firstEdge, queryGraph);

            double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    firstEdge.getBaseNode(), firstEdge.getAdjNode(), firstEdge);

            return distanceInOtherDirection / originalEdge.getDistance();
        }
        return 0D;
    }

    private static EdgeIteratorState findOriginalEdge(EdgeIteratorState edge, QueryGraph queryGraph) {
        EdgeIteratorState originalEdge;
        if (edge instanceof VirtualEdgeIteratorState) {
            originalEdge = queryGraph.getEdgeIteratorStateForKey(
                    ((VirtualEdgeIteratorState) edge).getOriginalEdgeKey());
        } else {
            originalEdge = edge;
        }
        return originalEdge;
    }

    private static double calculateDistanceFromVirtualNodeToNonVirtualNode(QueryGraph queryGraph, int virtualNode,
            int nodeToAvoid, EdgeIteratorState pathEdge) {
        EdgeExplorer edgeExplorer = queryGraph.createEdgeExplorer();

        double distanceInOtherDirection = 0D;
        boolean distanceInOtherDirectionCalculated = false;
        boolean distanceInOtherDirectionIsPositive = false;

        EdgeIterator edges = edgeExplorer.setBaseNode(virtualNode);
        int currentNodeToAvoid = nodeToAvoid;
        while (edges.next()) {
            if (edges.getAdjNode() != currentNodeToAvoid) {
                distanceInOtherDirection += edges.getDistance();

                // This assumes all edge lengths are > 0!
                distanceInOtherDirectionIsPositive = true;

                if (queryGraph.isVirtualNode(edges.getAdjNode())) {
                    // Search further
                    currentNodeToAvoid = edges.getBaseNode();
                    edges = edgeExplorer.setBaseNode(edges.getAdjNode());
                } else {
                    // Done
                    distanceInOtherDirectionCalculated = true;
                    break;
                }
            }
        }

        if (!distanceInOtherDirectionCalculated) {
            // This could be the case when an edge has one virtual node
            if (!distanceInOtherDirectionIsPositive) {
                EdgeIteratorState originalEdge = findOriginalEdge(pathEdge, queryGraph);
                distanceInOtherDirection = originalEdge.getDistance() -
                        pathEdge.getDistance();
            } else {
                throw new RoutingMapMatcherException("Unexpected: distance not correctly calculated");
            }
        }

        return distanceInOtherDirection;
    }

    public static double determineEndLinkFraction(EdgeIteratorState lastEdge, QueryGraph queryGraph) {
        if (queryGraph.isVirtualNode(lastEdge.getAdjNode())) {
            EdgeIteratorState originalEdge = findOriginalEdge(lastEdge, queryGraph);

            double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    lastEdge.getAdjNode(), lastEdge.getBaseNode(), lastEdge);

            return 1D - (distanceInOtherDirection / originalEdge.getDistance());
        }
        return 1D;
    }
}
