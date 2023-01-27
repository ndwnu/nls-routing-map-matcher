package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.storage.index.QueryResult.Position;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.GHPoint3D;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.TravelDirection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class PathUtil {

    private final GeometryFactory geometryFactory;

    public static final int ALL_NODES_MODE = 3;
    public static final int LINESTRING_MINIMUM_POINTS = 2;

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

    /**
     * This method determines the direction on which one can travel over a path. We always need to validate the
     * travel direction because of the way how graphhopper creates node indexes and always wants to store edges with
     * the lower node index followed by the higher node index:
     * https://github.com/graphhopper/graphhopper/blob/master/docs/core/technical.md
     * <p>
     * Even though we supply nodes in driving direction (base node to adjacent node), graphhopper sometimes decides
     * to return results in reverse direction.
     *
     * @param queryResult
     * @param flagEncoder
     * @return travel direction on this specific edge
     */
    public static TravelDirection determineEdgeDirection(final QueryResult queryResult,
            final LinkFlagEncoder flagEncoder) {
        final EdgeIteratorState edge = queryResult.getClosestEdge();

        final boolean edgeCanBeTraveledFromBaseToAdjacent = edge.get(flagEncoder.getAccessEnc());
        final boolean edgeCanBeTraveledFromAdjacentToBase = edge.getReverse(flagEncoder.getAccessEnc());

        if (edgeCanBeTraveledFromAdjacentToBase && edgeCanBeTraveledFromBaseToAdjacent) {
            return TravelDirection.BOTH_DIRECTIONS;
        } else if (edgeCanBeTraveledFromAdjacentToBase) {
            return TravelDirection.REVERSED;
        } else if (edgeCanBeTraveledFromBaseToAdjacent) {
            return TravelDirection.FORWARD;
        } else {
            throw new IllegalStateException("Edge has no travel direction");
        }
    }

    /**
     * Calculates the fraction (relative normalized distance, a number between 0 start and 1 end) from the start of
     * this segment to the snapped point on the path from the query result.
     * <p>
     * First a check is performed on a tower node match, because then we can either return 0 or 1 based on the wayIndex.
     * WayIndex indicates on which or after which node the snapped point is found on the edge, there for the start node
     * is 0 and if a tower node is not wayIndex 0 then it must be the adjacent end node.
     * <p>
     * Otherwise for each line segment the length is calculated, walking the edge path from base tower node (start) to
     * end adjacent node (total length)
     * snapped point (snapped point length)
     * <p>
     * Then the fraction is calculated by relating the snapped point length to the total length
     *
     * @param queryResult  the query result
     * @param distanceCalc the calculator to use
     * @return the fraction, relative distance on edge beween start 0 and snapped point
     */
    public double determineSnappedPointFraction(final QueryResult queryResult, final DistanceCalc distanceCalc,
                                                final LinkFlagEncoder flagEncoder) {
        // Find out after which point our snapped point snaps on the edge
        final int wayIndex = queryResult.getWayIndex();

        final Position snappedPosition = queryResult.getSnappedPosition();

        log.trace("Query result point snapped on node type: {}, closest edge {}", snappedPosition,
                queryResult.getClosestEdge());

        // Tower means at start or end, we can determine this without calculations
        if (snappedPosition == Position.TOWER) {
            if (wayIndex == 0) {
                log.debug("Found snapped position at base tower node, fraction: 0");
                return 0D;
            } else {
                log.debug("Found snapped position at adjacent tower node, fraction: 1");
                return 1D;
            }
        }

        // Closest edge found for our search point
        final EdgeIteratorState edge = queryResult.getClosestEdge();

        // We want to use all nodes, Base, pillar and adjacent nodes
        final PointList pointList = edge.fetchWayGeometry(ALL_NODES_MODE);

        if (pointList.getSize() < LINESTRING_MINIMUM_POINTS) {
            throw new IllegalStateException(
                    "pointList should contain at least two points, but contains: " + pointList.size());
        } else if (wayIndex >= pointList.getSize()) {
            throw new IndexOutOfBoundsException(
                    "Way index " + wayIndex + " out of bounds, point count: " + pointList.getSize());
        }

        final Iterator<GHPoint3D> it = pointList.iterator();

        GHPoint3D previous = it.next();

        double sumOfPathLengths = 0D;

        Double pathDistanceToSnappedPoint = null;

        int startNodeIndex = 0;

        // The closest point on our edge to our search point
        final GHPoint3D snappedPoint = queryResult.getSnappedPoint();

        while (it.hasNext()) {
            final GHPoint3D current = it.next();

            // If the start node index is the one after which we found the snapped point, calculate distance from
            // previous node to snapped point.
            if (wayIndex == startNodeIndex) {
                log.debug("Found snapped point after node {}", wayIndex);
                final double previousToSnappedPointDistance = distanceCalc.calcDist(previous.getLat(),
                        previous.getLon(), snappedPoint.getLat(), snappedPoint.getLon());

                log.trace("Distance from previous node (lat/lon) ({},{}) to snapped point ({},{}): {}",
                        previous.getLat(), previous.getLon(), snappedPoint.getLat(), snappedPoint.getLon(),
                        previousToSnappedPointDistance);

                pathDistanceToSnappedPoint = sumOfPathLengths + previousToSnappedPointDistance;
            }

            // Calculate distance from previous to current tower/pillar node
            sumOfPathLengths += distanceCalc.calcDist(previous.getLat(), previous.getLon(), current.getLat(),
                    current.getLon());

            log.trace("Length from start node to node index {} is {}", startNodeIndex + 1, sumOfPathLengths);

            // Prepare for next loop
            previous = current;
            startNodeIndex++;
        }

        if (pathDistanceToSnappedPoint == null) {
            throw new IllegalStateException("Failed to find path distance to snapped point");
        }

        final TravelDirection travelDirection = determineEdgeDirection(queryResult, flagEncoder);
        log.trace("Travel direction: {}", travelDirection);

        if (travelDirection == TravelDirection.BOTH_DIRECTIONS) {
            throw new IllegalStateException("Cannot determine travel direction");
        }

        double fraction = pathDistanceToSnappedPoint / sumOfPathLengths;
        if (travelDirection == TravelDirection.REVERSED) {
            log.trace("Reverse travel direction. Fraction will be inverted.");
            fraction = 1D - fraction;
        }

        log.trace("Total (geometrical) edge length: {}, snapped point path length {}. Fraction: {}", sumOfPathLengths,
                pathDistanceToSnappedPoint, fraction);

        return fraction;
    }

    public double determineStartLinkFraction(final EdgeIteratorState firstEdge, final QueryGraph queryGraph) {
        final double startLinkFraction;
        if (queryGraph.isVirtualNode(firstEdge.getBaseNode())) {
            final EdgeIteratorState originalEdge = findOriginalEdge(firstEdge, queryGraph);

            final double distanceInOtherDirection = calculateDistanceFromVirtualNodeToNonVirtualNode(queryGraph,
                    firstEdge.getBaseNode(), firstEdge.getAdjNode(), firstEdge);

            startLinkFraction = distanceInOtherDirection / originalEdge.getDistance();
        } else {
            startLinkFraction = 0D;
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
                throw new IllegalStateException(
                        "Unexpected state: at least one node of a virtual edge should be virtual");
            }
        } else {
            originalEdge = edge;
        }
        return originalEdge;
    }

    private double calculateDistanceFromVirtualNodeToNonVirtualNode(final QueryGraph queryGraph, final int virtualNode,
                                                                    final int nodeToAvoid, final EdgeIteratorState pathEdge) {
        final EdgeExplorer edgeExplorer = queryGraph.createEdgeExplorer();

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

            endLinkFraction = 1D - (distanceInOtherDirection / originalEdge.getDistance());
        } else {
            endLinkFraction = 1D;
        }

        return endLinkFraction;
    }
}
