package nu.ndw.nls.routingmapmatcher.isochrone.mappers;


import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.util.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.locationtech.jts.geom.LineString;

@RequiredArgsConstructor
public class IsochroneMatchMapper {

    private final EncodingManager encodingManager;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final PointListUtil pointListUtil;

    /**
     * Maps an IsoLabel to an IsochroneMatch with cropped geometries aligned to travelling direction and respective
     * start and end fractions.
     *
     * @param isoLabel the iso label to map
     * @return an instance of IsochroneMatch
     */
    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel, double maxDistance, QueryGraph queryGraph,
            EdgeIteratorState startEdge) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode());
        // Here, the reversed boolean indicates the direction of travelling along the edge with respect to the original
        // alignment of the geometry (can be backward for bidirectional edges or for upstream isochrone searches).
        boolean reversed = edgeIteratorStateReverseExtractor.hasReversed(currentEdge);
        IntEncodedValue idEnc = encodingManager.getIntEncodedValue(WAY_ID_KEY);
        int roadSectionId = currentEdge.get(idEnc);
        double totalDistanceTravelled = isoLabel.getDistance();
        // This is the entire way geometry, except for the start segment, which is split up at the start point.
        LineString isoLabelWayGeometry = pointListUtil.toLineString(currentEdge.fetchWayGeometry(FetchMode.ALL));

        // The start segment in the iso label is split into 2 sections, in case of bidirectional roads in opposite
        // directions as indicated by the edgeDirection.
        // Here the fractions are calculated based on the entire start-segment geometry with respect to the partial
        // edges.
        boolean isStartSegment = isStartSegment(roadSectionId, startEdge);
        LineString fullGeometry = isStartSegment
                ? getStartSegmentWayGeometryInTravelDirection(reversed, startEdge) : isoLabelWayGeometry;

        // If the total distance travelled exceeds the maximum distance, cut the linestring accordingly.
        boolean isEndSegment = totalDistanceTravelled > maxDistance;
        LineString partialGeometry = isEndSegment
                ? calculatePartialGeometry(isoLabelWayGeometry, totalDistanceTravelled, maxDistance)
                : isoLabelWayGeometry;

        double startFraction = isStartSegment
                ? FractionAndDistanceCalculator.calculateFractionAndDistance(fullGeometry,
                isoLabelWayGeometry.getStartPoint().getCoordinate()).getFraction() : 0.0;
        double endFraction = isStartSegment || isEndSegment
                ? FractionAndDistanceCalculator.calculateFractionAndDistance(fullGeometry,
                partialGeometry.getEndPoint().getCoordinate()).getFraction() : 1.0;

        return IsochroneMatch.builder()
                .matchedLinkId(roadSectionId)
                .startFraction(startFraction)
                .endFraction(endFraction)
                .reversed(reversed)
                .geometry(partialGeometry)
                .build();
    }

    public boolean isStartSegment(int roadSectionId, EdgeIteratorState startEdge) {
        IntEncodedValue idEnc = encodingManager.getIntEncodedValue(WAY_ID_KEY);
        int startSegmentId = startEdge.get(idEnc);
        return roadSectionId == startSegmentId;
    }

    private LineString getStartSegmentWayGeometryInTravelDirection(boolean reversed, EdgeIteratorState startEdge) {
        LineString startSegmentWayGeometry = pointListUtil.toLineString(startEdge.fetchWayGeometry(FetchMode.ALL));
        return edgeIteratorStateReverseExtractor.hasReversed(startEdge) != reversed ? startSegmentWayGeometry.reverse()
                : startSegmentWayGeometry;
    }

    private LineString calculatePartialGeometry(LineString edgeGeometry, double totalDistanceTravelled,
            double maxDistance) {
        double isoLabelEdgeGeometryDistance = FractionAndDistanceCalculator.calculateLengthInMeters(edgeGeometry);
        double partialFraction = (maxDistance - totalDistanceTravelled + isoLabelEdgeGeometryDistance)
                / isoLabelEdgeGeometryDistance;
        return FractionAndDistanceCalculator.getSubLineString(edgeGeometry, partialFraction);
    }
}
