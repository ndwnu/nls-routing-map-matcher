package nu.ndw.nls.routingmapmatcher.util;

import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

public final class PathUtil {

    private static final EdgeIteratorStateReverseExtractor EDGE_ITERATOR_STATE_REVERSE_EXTRACTOR =
            new EdgeIteratorStateReverseExtractor();
    private static final PointListUtil POINT_LIST_UTIL = new PointListUtil();

    private PathUtil() {
        // Util class
    }

    public static List<MatchedLink> determineMatchedLinks(EncodingManager encodingManager,
            Collection<EdgeIteratorState> edges) {
        List<MatchedLink> matchedLinks = new ArrayList<>();
        for (EdgeIteratorState edge : edges) {
            int matchedLinkId = edge.get(encodingManager.getIntEncodedValue(WAY_ID_KEY));
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
     * <a href="https://github.com/graphhopper/graphhopper/blob/master/docs/core/technical.md">Technical</a>
     * <p>
     * Even though we supply nodes in driving direction (base node to adjacent node), graphhopper sometimes decides to
     * return results in reverse direction.
     *
     * @return travel direction on this specific edge
     */
    public static EdgeIteratorTravelDirection determineEdgeDirection(EdgeIteratorState edge,
            EncodingManager encodingManager, String vehicleName) {
        boolean edgeCanBeTraveledFromBaseToAdjacent = edge.get(
                encodingManager.getBooleanEncodedValue(VehicleAccess.key(vehicleName)));
        boolean edgeCanBeTraveledFromAdjacentToBase = edge.getReverse(
                encodingManager.getBooleanEncodedValue(VehicleAccess.key(vehicleName)));
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

            LineString originalGeometry = POINT_LIST_UTIL.toLineString(originalEdge.fetchWayGeometry(FetchMode.ALL));
            Coordinate coordinate = POINT_LIST_UTIL.toLineString(firstEdge.fetchWayGeometry(FetchMode.ALL))
                    .getStartPoint().getCoordinate();
            return FractionAndDistanceCalculator.calculateFractionAndDistance(originalGeometry, coordinate)
                    .getFraction();
        }
        return 0D;
    }

    public static double determineEndLinkFraction(EdgeIteratorState lastEdge, QueryGraph queryGraph) {
        if (queryGraph.isVirtualNode(lastEdge.getAdjNode())) {
            EdgeIteratorState originalEdge = findOriginalEdge(lastEdge, queryGraph);

            LineString originalGeometry = POINT_LIST_UTIL.toLineString(originalEdge.fetchWayGeometry(FetchMode.ALL));
            Coordinate coordinate = POINT_LIST_UTIL.toLineString(lastEdge.fetchWayGeometry(FetchMode.ALL))
                    .getEndPoint().getCoordinate();
            return FractionAndDistanceCalculator.calculateFractionAndDistance(originalGeometry, coordinate)
                    .getFraction();
        }
        return 1D;
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
}
