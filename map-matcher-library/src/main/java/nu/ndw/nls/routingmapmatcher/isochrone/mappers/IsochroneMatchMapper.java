package nu.ndw.nls.routingmapmatcher.isochrone.mappers;


import static nu.ndw.nls.routingmapmatcher.network.model.Link.REVERSED_LINK_ID;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;

import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.IsochroneParentLink;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.locationtech.jts.geom.LineString;

@RequiredArgsConstructor
public class IsochroneMatchMapper {

    private final EncodingManager encodingManager;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final PointListUtil pointListUtil;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    /**
     * Maps an IsoLabel to an IsochroneMatch with cropped geometries aligned to travelling direction and respective
     * start and end fractions.
     *
     * @param isoLabel    the iso label to map
     * @param reverseFlow whether the isochrone path is upstream (true) or downstream (false)
     * @return an instance of IsochroneMatch
     */
    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel, double maxDistance, QueryGraph queryGraph,
            EdgeIteratorState startEdge, boolean reverseFlow) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode());
        // Here, the reversed boolean indicates the direction of travelling along the edge with respect to the original
        // alignment of the geometry (can be backward for bidirectional edges or for upstream isochrone searches).
        boolean reversed = edgeIteratorStateReverseExtractor.hasReversed(currentEdge);
        int matchedLinkId = getLinkIdInDirection(currentEdge, reversed, reverseFlow);
        double totalDistanceTravelled = isoLabel.getDistance();
        // This is the entire way geometry, except for the start segment, which is split up at the start point.
        LineString isoLabelWayGeometry = pointListUtil.toLineString(currentEdge.fetchWayGeometry(FetchMode.ALL));

        // The start segment in the iso label is split into 2 sections, in case of bidirectional roads in opposite
        // directions as indicated by the edgeDirection.
        // Here the fractions are calculated based on the entire start-segment geometry with respect to the partial
        // edges.
        boolean isStartSegment = isStartSegment(matchedLinkId, startEdge, reverseFlow);
        LineString fullGeometry = isStartSegment
                ? getStartSegmentWayGeometryInTravelDirection(reversed, startEdge) : isoLabelWayGeometry;

        // If the total distance travelled exceeds the maximum distance, cut the linestring accordingly.
        boolean isEndSegment = totalDistanceTravelled > maxDistance;
        LineString partialGeometry = isEndSegment
                ? calculatePartialGeometry(isoLabelWayGeometry, totalDistanceTravelled, maxDistance)
                : isoLabelWayGeometry;

        double startFraction = isStartSegment
                ? fractionAndDistanceCalculator.calculateFractionAndDistance(fullGeometry,
                isoLabelWayGeometry.getStartPoint().getCoordinate()).getFraction() : 0.0;
        double endFraction = isStartSegment || isEndSegment
                ? fractionAndDistanceCalculator.calculateFractionAndDistance(fullGeometry,
                partialGeometry.getEndPoint().getCoordinate()).getFraction() : 1.0;

        // Invert values for upstream.
        double correctedStartFraction = reverseFlow ? (1 - endFraction) : startFraction;
        double correctedEndFraction = reverseFlow ? (1 - startFraction) : endFraction;
        boolean correctedReversed = (reversed != reverseFlow) && !hasReversedLinkId(currentEdge);
        LineString correctedGeometry = reverseFlow ? partialGeometry.reverse() : partialGeometry;

        return IsochroneMatch
                .builder()
                .edgeKey(currentEdge.getEdgeKey())
                .matchedLinkId(matchedLinkId)
                .startFraction(correctedStartFraction)
                .endFraction(correctedEndFraction)
                .reversed(correctedReversed)
                .parentLink(createParentLink(isoLabel, queryGraph, reverseFlow))
                .geometry(correctedGeometry)
                .build();
    }

    private IsochroneParentLink createParentLink(IsoLabel isoLabel, QueryGraph queryGraph, boolean reverseFlow) {
        if (isoLabel.parentIsRoot()) {
            return null;
        }

        EdgeIteratorState parentEdge = queryGraph.getEdgeIteratorState(isoLabel.getParent().getEdge(),
                isoLabel.getParent().getNode());
        boolean reversed = edgeIteratorStateReverseExtractor.hasReversed(parentEdge);
        int linkId = getLinkIdInDirection(parentEdge, reversed, reverseFlow);
        return IsochroneParentLink
                .builder()
                .linkId(linkId)
                .reversed((reversed != reverseFlow) && !hasReversedLinkId(parentEdge))
                .build();
    }

    private int getLinkId(EdgeIteratorState edge) {
        return edge.get(encodingManager.getIntEncodedValue(WAY_ID_KEY));
    }

    private int getReversedLinkId(EdgeIteratorState edge) {
        return edge.get(encodingManager.getIntEncodedValue(REVERSED_LINK_ID));
    }

    private boolean hasReversedLinkId(EdgeIteratorState edge) {
        return getReversedLinkId(edge) > 0;
    }

    private int getLinkIdInDirection(EdgeIteratorState startEdge, boolean reversed, boolean reverseFlow) {
        return (reversed != reverseFlow) && hasReversedLinkId(startEdge)
                ? getReversedLinkId(startEdge)
                : getLinkId(startEdge);
    }

    private boolean isStartSegment(int roadSectionId, EdgeIteratorState startEdge, boolean reverseFlow) {
        boolean reversed = edgeIteratorStateReverseExtractor.hasReversed(startEdge);
        int startSegmentId = getLinkIdInDirection(startEdge, reversed, reverseFlow);
        return roadSectionId == startSegmentId;
    }

    private LineString getStartSegmentWayGeometryInTravelDirection(boolean reversed, EdgeIteratorState startEdge) {
        LineString startSegmentWayGeometry = pointListUtil.toLineString(startEdge.fetchWayGeometry(FetchMode.ALL));
        return edgeIteratorStateReverseExtractor.hasReversed(startEdge) != reversed ? startSegmentWayGeometry.reverse()
                : startSegmentWayGeometry;
    }

    private LineString calculatePartialGeometry(LineString edgeGeometry, double totalDistanceTravelled,
            double maxDistance) {
        double isoLabelEdgeGeometryDistance = fractionAndDistanceCalculator.calculateLengthInMeters(edgeGeometry);
        double partialFraction = (maxDistance - totalDistanceTravelled + isoLabelEdgeGeometryDistance)
                / isoLabelEdgeGeometryDistance;
        return fractionAndDistanceCalculator.getSubLineString(edgeGeometry, partialFraction);
    }
}
