package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;

import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.querygraph.VirtualEdgeIteratorState;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

@Slf4j
public class PathUtil {

    private static final int MINIMUM_LENGTH = 2;
    private static final int DIMENSIONS = 2;
    private static final int MEASURES = 0;
    private static final int KEY_FACTOR = 2;

    private final GeometryFactory geometryFactory;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor =
            new EdgeIteratorStateReverseExtractor();

    public PathUtil(GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    public LineString createLineString(PointList points) {
        LineString lineString;
        if (points.size() > 1) {
            PackedCoordinateSequence.Double coordinateSequence =
                    new PackedCoordinateSequence.Double(points.size(), DIMENSIONS, MEASURES);
            for (int index = 0; index < points.size(); index++) {
                coordinateSequence.setOrdinate(index, 0, points.getLon(index));
                coordinateSequence.setOrdinate(index, 1, points.getLat(index));
            }
            lineString = geometryFactory.createLineString(coordinateSequence);
        } else if (points.size() == 1) {
            PackedCoordinateSequence.Double coordinateSequence =
                    new PackedCoordinateSequence.Double(MINIMUM_LENGTH, DIMENSIONS, MEASURES);
            coordinateSequence.setOrdinate(0, 0, points.getLon(0));
            coordinateSequence.setOrdinate(0, 1, points.getLat(0));
            coordinateSequence.setOrdinate(1, 0, points.getLon(0));
            coordinateSequence.setOrdinate(1, 1, points.getLat(0));
            lineString = geometryFactory.createLineString(coordinateSequence);
        } else {
            throw new RoutingMapMatcherException("Unexpected: no points");
        }
        return lineString;
    }

    public List<MatchedLink> determineMatchedLinks(EncodingManager encodingManager,
            Collection<EdgeIteratorState> edges) {
        List<MatchedLink> matchedLinks = new ArrayList<>(edges.size());
        Integer previousMatchedLinkId = null;
        for (EdgeIteratorState edge : edges) {
            Integer matchedLinkId = edge.get(encodingManager.getIntEncodedValue(ID_NAME));
            if (previousMatchedLinkId == null || !previousMatchedLinkId.equals(matchedLinkId)) {
                MatchedLink matchedLink = MatchedLink.builder()
                        .linkId(matchedLinkId)
                        .reversed(edgeIteratorStateReverseExtractor.hasReversed(edge))
                        .build();
                matchedLinks.add(matchedLink);
            }
            previousMatchedLinkId = matchedLinkId;
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

    public double determineStartLinkFraction(EdgeIteratorState firstEdge, QueryGraph queryGraph) {
        if (queryGraph.isVirtualNode(firstEdge.getBaseNode())) {
            EdgeIteratorState originalEdge = findOriginalEdge(firstEdge, queryGraph);

            double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    firstEdge.getBaseNode(), firstEdge.getAdjNode(), firstEdge);

            return distanceInOtherDirection / originalEdge.getDistance();
        }
        return 0D;
    }

    private EdgeIteratorState findOriginalEdge(EdgeIteratorState edge, QueryGraph queryGraph) {
        EdgeIteratorState originalEdge;
        if (edge instanceof VirtualEdgeIteratorState) {
            originalEdge = queryGraph.getEdgeIteratorStateForKey(
                    ((VirtualEdgeIteratorState) edge).getOriginalEdgeKey());
        } else {
            originalEdge = edge;
        }
        return originalEdge;
    }

    private double calculateDistanceFromVirtualNodeToNonVirtualNode(QueryGraph queryGraph, int virtualNode,
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
            // This could be the case when an edge has exactly one virtual node and that edge has the same node as
            // start node and end node; in this situation the value of distanceInOtherDirection should be 0
            if (!distanceInOtherDirectionIsPositive) {
                int originalEdgeKey = ((VirtualEdgeIteratorState) queryGraph.getEdgeIteratorStateForKey(
                        virtualNode * KEY_FACTOR))
                        .getOriginalEdgeKey();
                distanceInOtherDirection = queryGraph.getEdgeIteratorStateForKey(originalEdgeKey).getDistance() -
                        pathEdge.getDistance();
            } else {
                throw new RoutingMapMatcherException("Unexpected: distance not correctly calculated");
            }
        }

        return distanceInOtherDirection;
    }

    public double determineEndLinkFraction(EdgeIteratorState lastEdge, QueryGraph queryGraph) {
        if (queryGraph.isVirtualNode(lastEdge.getAdjNode())) {
            EdgeIteratorState originalEdge = findOriginalEdge(lastEdge, queryGraph);

            double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    lastEdge.getAdjNode(), lastEdge.getBaseNode(), lastEdge);

            return 1D - (distanceInOtherDirection / originalEdge.getDistance());
        }
        return 1D;
    }
}
