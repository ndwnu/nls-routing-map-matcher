package nl.dat.routingmapmatcher.util;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.PointList;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;

import nl.dat.routingmapmatcher.exceptions.RoutingMapMatcherException;
import nl.dat.routingmapmatcher.graphhopper.NdwGraphHopper;
import nl.dat.routingmapmatcher.graphhopper.NdwLinkFlagEncoder;
import nl.dat.routingmapmatcher.graphhopper.NdwLinkProperties;

public class PathUtil {

  private final GeometryFactory geometryFactory;

  public PathUtil(final GeometryFactory geometryFactory) {
    this.geometryFactory = geometryFactory;
  }

  public LineString createLineString(final PointList points) {
    final LineString lineString;
    if (points.size() > 1) {
      final PackedCoordinateSequence.Double coordinateSequence = new PackedCoordinateSequence.Double(points.size(), 2);
      for (int index = 0; index < points.size(); index++) {
        coordinateSequence.setOrdinate(index, 0, points.getLongitude(index));
        coordinateSequence.setOrdinate(index, 1, points.getLatitude(index));
      }
      lineString = geometryFactory.createLineString(coordinateSequence);
    } else if (points.size() == 1) {
      final PackedCoordinateSequence.Double coordinateSequence = new PackedCoordinateSequence.Double(2, 2);
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

  public List<Integer> determineNdwLinkIds(final NdwGraphHopper ndwNetwork, final NdwLinkFlagEncoder flagEncoder,
      final List<EdgeIteratorState> edges) {
    final List<Integer> ndwLinkIds = new ArrayList<>(edges.size());
    Integer previousNdwLinkId = null;
    for (final EdgeIteratorState edge : edges) {
      final long flags = edge.getFlags();
      final boolean reversed = flagEncoder.isReversed(flags);
      final int index = flagEncoder.getIndex(flags);
      final NdwLinkProperties linkProperties = ndwNetwork.getLinkProperties(index);
      final Integer ndwLinkId;
      if (reversed) {
        ndwLinkId = linkProperties.getBackwardId();
      } else {
        ndwLinkId = linkProperties.getForwardId();
      }
      if (previousNdwLinkId == null || !previousNdwLinkId.equals(ndwLinkId)) {
        ndwLinkIds.add(ndwLinkId);
      }
      previousNdwLinkId = ndwLinkId;
    }
    return ndwLinkIds;
  }

  public double determineStartLinkFraction(final EdgeIteratorState firstEdge, final QueryGraph queryGraph) {
    double startLinkFraction;
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
        throw new IllegalStateException("Unexpected state: at least one node of a virtual edge should be virtual");
      }
    } else {
      originalEdge = edge;
    }
    return originalEdge;
  }

  private double calculateDistanceFromVirtualNodeToNonVirtualNode(final QueryGraph queryGraph, final int virtualNode,
      final int nodeToAvoid, final EdgeIteratorState pathEdge) {
    final EdgeExplorer edgeExplorer = queryGraph.createEdgeExplorer();

    double distanceInOtherDirection = 0.0;
    boolean distanceInOtherDirectionCalculated = false;

    EdgeIterator edges = edgeExplorer.setBaseNode(virtualNode);
    int currentNodeToAvoid = nodeToAvoid;
    while (edges.next()) {
      if (edges.getAdjNode() != currentNodeToAvoid) {
        distanceInOtherDirection += edges.getDistance();
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
      if (distanceInOtherDirection == 0.0) {
        distanceInOtherDirection = queryGraph.getOriginalEdgeFromVirtNode(virtualNode).getDistance() -
            pathEdge.getDistance();
      } else {
        throw new RoutingMapMatcherException("Unexpected: distance not correctly calculated");
      }
    }

    return distanceInOtherDirection;
  }

  public double determineEndLinkFraction(final EdgeIteratorState lastEdge, final QueryGraph queryGraph) {
    double endLinkFraction;
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
