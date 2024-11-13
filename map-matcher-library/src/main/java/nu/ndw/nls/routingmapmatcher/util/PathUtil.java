package nu.ndw.nls.routingmapmatcher.util;

import static nu.ndw.nls.routingmapmatcher.network.model.Link.REVERSED_LINK_ID;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;

import com.conductor.stream.utils.OrderedStreamUtils;
import com.graphhopper.routing.ev.BooleanEncodedValue;
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
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedEdgeLink;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

public final class PathUtil {

    private static final EdgeIteratorStateReverseExtractor EDGE_ITERATOR_STATE_REVERSE_EXTRACTOR = new EdgeIteratorStateReverseExtractor();

    //todo : replace logic in this class to spring managed service
    private static final PointListUtil POINT_LIST_UTIL = new PointListUtil(new GeometryFactoryWgs84());

    private PathUtil() {
        // Util class
    }

    public static List<MatchedEdgeLink> determineMatchedLinks(EncodingManager encodingManager,
            FractionAndDistanceCalculator fractionAndDistanceCalculator,
            Collection<EdgeIteratorState> edges) {
        List<MatchedEdgeLink> matchedEdgeLinks = new ArrayList<>();
        for (EdgeIteratorState edge : edges) {
            boolean reversed = EDGE_ITERATOR_STATE_REVERSE_EXTRACTOR.hasReversed(edge);
            int matchedLinkId = reversed && hasReversedLinkId(encodingManager, edge)
                    ? getReversedLinkId(encodingManager, edge)
                    : getLinkId(encodingManager, edge);
            LineString lineString = POINT_LIST_UTIL.toLineString(edge.fetchWayGeometry(FetchMode.ALL));
            matchedEdgeLinks.add(MatchedEdgeLink.builder()
                    .linkId(matchedLinkId)
                    .distance(fractionAndDistanceCalculator.calculateLengthInMeters(lineString))
                    .reversed(reversed && !hasReversedLinkId(encodingManager, edge))
                    .build());
        }

        // Road sections can be subdivided into multiple virtual edges because of via points in routing and match requests.
        // See https://github.com/graphhopper/graphhopper/blob/master/docs/core/low-level-api.md#what-are-virtual-edges-and-nodes
        // In the output we need to group over road sections with the same linkId and direction and take the sum of the distances.
        return OrderedStreamUtils.groupBy(matchedEdgeLinks.stream(), PathUtil::byLinkIdAndDirection)
                .map(PathUtil::reduceWithSummedDistance)
                .toList();
    }

    private static String byLinkIdAndDirection(MatchedEdgeLink matchedEdgeLink) {
        return matchedEdgeLink.getLinkId() + "_" + matchedEdgeLink.isReversed();
    }

    private static MatchedEdgeLink reduceWithSummedDistance(List<MatchedEdgeLink> grouped) {
        double totalDistance = grouped.stream()
                .mapToDouble(MatchedEdgeLink::getDistance)
                .sum();
        return MatchedEdgeLink
                .builder()
                .distance(totalDistance)
                .linkId(grouped.getFirst().getLinkId())
                .reversed(grouped.getFirst().isReversed())
                .build();
    }

    private static int getLinkId(EncodingManager encodingManager, EdgeIteratorState edge) {
        return edge.get(encodingManager.getIntEncodedValue(WAY_ID_KEY));
    }

    private static int getReversedLinkId(EncodingManager encodingManager, EdgeIteratorState edge) {
        return edge.get(encodingManager.getIntEncodedValue(REVERSED_LINK_ID));
    }

    private static boolean hasReversedLinkId(EncodingManager encodingManager, EdgeIteratorState edge) {
        return getReversedLinkId(encodingManager, edge) > 0L;
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
    public static EdgeIteratorTravelDirection determineEdgeDirection(
            EdgeIteratorState edge,
            EncodingManager encodingManager,
            String vehicleName
    ) {
        BooleanEncodedValue vehicleAccessEncodedValue = encodingManager.getBooleanEncodedValue(VehicleAccess.key(vehicleName));
        boolean edgeCanBeTraveledFromBaseToAdjacent = edge.get(vehicleAccessEncodedValue);
        boolean edgeCanBeTraveledFromAdjacentToBase = edge.getReverse(vehicleAccessEncodedValue);
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

    public static double determineStartLinkFraction(
            EdgeIteratorState firstEdge,
            QueryGraph queryGraph,
            FractionAndDistanceCalculator fractionAndDistanceCalculator
    ) {
        if (!queryGraph.isVirtualNode(firstEdge.getBaseNode())) {
            return 0D;
        }

        EdgeIteratorState originalEdge = findOriginalEdge(firstEdge, queryGraph);

        LineString originalGeometry = POINT_LIST_UTIL.toLineString(originalEdge.fetchWayGeometry(FetchMode.ALL));
        Coordinate coordinate = POINT_LIST_UTIL.toLineString(firstEdge.fetchWayGeometry(FetchMode.ALL))
                .getStartPoint().getCoordinate();
        return fractionAndDistanceCalculator.calculateFractionAndDistance(originalGeometry, coordinate)
                .getFraction();
    }

    public static double determineEndLinkFraction(
            EdgeIteratorState lastEdge,
            QueryGraph queryGraph,
            FractionAndDistanceCalculator fractionAndDistanceCalculator
    ) {
        if (!queryGraph.isVirtualNode(lastEdge.getAdjNode())) {
            return 1D;
        }

        EdgeIteratorState originalEdge = findOriginalEdge(lastEdge, queryGraph);

        LineString originalGeometry = POINT_LIST_UTIL.toLineString(originalEdge.fetchWayGeometry(FetchMode.ALL));
        Coordinate coordinate = POINT_LIST_UTIL.toLineString(lastEdge.fetchWayGeometry(FetchMode.ALL))
                .getEndPoint().getCoordinate();
        return fractionAndDistanceCalculator.calculateFractionAndDistance(originalGeometry, coordinate)
                .getFraction();
    }

    private static EdgeIteratorState findOriginalEdge(EdgeIteratorState edge, QueryGraph queryGraph) {
        if (edge instanceof VirtualEdgeIteratorState state) {
            return queryGraph.getEdgeIteratorStateForKey(state.getOriginalEdgeKey());
        }

        return edge;
    }
}
