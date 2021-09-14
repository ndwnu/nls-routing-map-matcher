package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PathUtil {

    private final GeometryFactory geometryFactory;

    public PathUtil(final GeometryFactory geometryFactory) {
        this.geometryFactory = geometryFactory;
    }

    public LineString createLineString(final PointList points) {
        final LineString lineString;
        if (points.size() > 1) {
            final PackedCoordinateSequence.Double coordinateSequence =
                    new PackedCoordinateSequence.Double(points.size(), 2, 0);
            for (int index = 0; index < points.size(); index++) {
                coordinateSequence.setOrdinate(index, 0, points.getLongitude(index));
                coordinateSequence.setOrdinate(index, 1, points.getLatitude(index));
            }
            lineString = geometryFactory.createLineString(coordinateSequence);
        } else if (points.size() == 1) {
            final PackedCoordinateSequence.Double coordinateSequence =
                    new PackedCoordinateSequence.Double(2, 2, 0);
            coordinateSequence.setOrdinate(0, 0, points.getLongitude(0));
            coordinateSequence.setOrdinate(0, 1, points.getLatitude(0));
            coordinateSequence.setOrdinate(1, 0, points.getLongitude(0));
            coordinateSequence.setOrdinate(1, 1, points.getLatitude(0));
            lineString = geometryFactory.createLineString(coordinateSequence);
        } else {
            throw new RoutingMapMatcherException("Unexpected: no points");
        }
        return lineString;
    }

    public List<Integer> determineMatchedLinkIds(final LinkFlagEncoder flagEncoder,
                                                 final Collection<EdgeIteratorState> edges) {
        final List<Integer> matchedLinkIds = new ArrayList<>(edges.size());
        Integer previousMatchedLinkId = null;
        for (final EdgeIteratorState edge : edges) {
            final IntsRef flags = edge.getFlags();
            final Integer matchedLinkId = flagEncoder.getId(flags);
            if (previousMatchedLinkId == null || !previousMatchedLinkId.equals(matchedLinkId)) {
                matchedLinkIds.add(matchedLinkId);
            }
            previousMatchedLinkId = matchedLinkId;
        }
        return matchedLinkIds;
    }

    public double determineStartLinkFraction(final EdgeIteratorState firstEdge, final QueryGraph queryGraph) {
        final double startLinkFraction;
        if (queryGraph.isVirtualNode(firstEdge.getBaseNode())) {
            final EdgeIteratorState originalEdge = findOriginalEdge(firstEdge, queryGraph);

            final double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    firstEdge.getBaseNode(), firstEdge.getAdjNode(), firstEdge);

            startLinkFraction = distanceInOtherDirection / originalEdge.getDistance();
        } else {
            startLinkFraction = 0.0;
        }

        return startLinkFraction;
    }

    private EdgeIteratorState findOriginalEdge(final EdgeIteratorState edge, final QueryGraph queryGraph) {
        final EdgeIteratorState originalEdge;
        if (queryGraph.isVirtualEdge(edge.getEdge())) {
            if (queryGraph.isVirtualNode(edge.getBaseNode())) {
                originalEdge = queryGraph.getOriginalEdgeFromVirtNode(edge.getBaseNode());
            } else if (queryGraph.isVirtualNode(edge.getAdjNode())) {
                originalEdge = queryGraph.getOriginalEdgeFromVirtNode(edge.getAdjNode());
            } else {
                throw new IllegalStateException
                        ("Unexpected state: at least one node of a virtual edge should be virtual");
            }
        } else {
            originalEdge = edge;
        }
        return originalEdge;
    }

    private double calculateDistanceFromVirtualNodeToNonVirtualNode(final QueryGraph queryGraph, final int virtualNode,
                                                                    final int nodeToAvoid,
                                                                    final EdgeIteratorState pathEdge) {
        final EdgeExplorer edgeExplorer = queryGraph.createEdgeExplorer();

        double distanceInOtherDirection = 0.0;
        boolean distanceInOtherDirectionCalculated = false;
        boolean distanceInOtherDirectionIsPositive = false;

        EdgeIterator edges = edgeExplorer.setBaseNode(virtualNode);
        int currentNodeToAvoid = nodeToAvoid;
        while (edges.next()) {
            if (edges.getAdjNode() != currentNodeToAvoid) {
                distanceInOtherDirection += edges.getDistance();

                // This assumes all edge lengths are > 0.0!
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
            // start node and end node; in this situation the value of distanceInOtherDirection should be 0.0
            if (!distanceInOtherDirectionIsPositive) {
                distanceInOtherDirection = queryGraph.getOriginalEdgeFromVirtNode(virtualNode).getDistance() -
                        pathEdge.getDistance();
            } else {
                throw new RoutingMapMatcherException("Unexpected: distance not correctly calculated");
            }
        }

        return distanceInOtherDirection;
    }

    public double determineEndLinkFraction(final EdgeIteratorState lastEdge, final QueryGraph queryGraph) {
        final double endLinkFraction;
        if (queryGraph.isVirtualNode(lastEdge.getAdjNode())) {
            final EdgeIteratorState originalEdge = findOriginalEdge(lastEdge, queryGraph);

            final double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    lastEdge.getAdjNode(), lastEdge.getBaseNode(), lastEdge);

            endLinkFraction = 1.0 - (distanceInOtherDirection / originalEdge.getDistance());
        } else {
            endLinkFraction = 1.0;
        }

        return endLinkFraction;
    }
}
