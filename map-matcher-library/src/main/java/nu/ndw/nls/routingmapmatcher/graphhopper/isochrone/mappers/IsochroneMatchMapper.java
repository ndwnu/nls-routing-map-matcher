package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;


import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.WAY_ID;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.ShortestPathTree.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

@RequiredArgsConstructor
public class IsochroneMatchMapper {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int ROUNDING_DECIMAL_PLACES = 12;
    private final CrsTransformer crsTransformer;
    private final EncodingManager encodingManager;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;

    /**
     * Maps an IsoLabel to an IsochroneMatch with cropped geometries aligned to travelling direction and respective
     * start and end fractions.
     *
     * @param isoLabel the iso label to map
     * @return an instance of IsochroneMatch
     */
    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel, double maxDistance, QueryGraph queryGraph,
            Snap startSegment) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.node);
        /*  Here the reversed boolean indicates the direction of travelling along the edge
         *  with respect to the original alignment of the geometry
         * (can be backward for bidirectional edges or for upstream isochrone searches).
         */
        boolean reversed = edgeIteratorStateReverseExtractor.hasReversed(currentEdge);
        IntEncodedValue idEnc = encodingManager.getIntEncodedValue(WAY_ID.getKey());
        int roadSectionId = currentEdge.get(idEnc);
        double totalDistanceTravelled = isoLabel.distance;
        double startFraction = 0D;
        double endFraction = 1D;
        // This is the entire way geometry except for the start segment which is split up at the start point
        LineString isoLabelWayGeometry = currentEdge
                .fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION);
        double isoLabelEdgeGeometryDistance = FractionAndDistanceCalculator.calculateLengthInMeters(
                isoLabelWayGeometry);

        /*
         *   The start segment in the iso-label is split into 2 sections, in case of bidirectional roads
         *   in opposite directions as indicated by the edgeDirection.
         *   Here the fractions are calculated based on the entire start-segment geometry
         *   with respect to the partial edges.
         * */
        if (isStartSegment(roadSectionId, startSegment)) {
            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
            if (totalDistanceTravelled > maxDistance) {
                isoLabelWayGeometry = calculatePartialGeometry(isoLabelWayGeometry, isoLabelEdgeGeometryDistance,
                        totalDistanceTravelled, maxDistance);
            }
            LineString startSegmentWayGeometryInTravelDirection = getStartSegmentWayGeometryInTravelDirection(reversed,
                    startSegment);

            startFraction = FractionAndDistanceCalculator.calculateFractionAndDistance(
                            startSegmentWayGeometryInTravelDirection,
                            isoLabelWayGeometry.getStartPoint().getCoordinate())
                    .getFraction();
            endFraction = FractionAndDistanceCalculator.calculateFractionAndDistance(
                            startSegmentWayGeometryInTravelDirection,
                            isoLabelWayGeometry.getEndPoint().getCoordinate())
                    .getFraction();
            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
        } else if (totalDistanceTravelled > maxDistance) {
            LineString originalGeometry = (LineString) isoLabelWayGeometry.copy();
            isoLabelWayGeometry = calculatePartialGeometry(isoLabelWayGeometry, isoLabelEdgeGeometryDistance,
                    totalDistanceTravelled, maxDistance);
            endFraction = FractionAndDistanceCalculator.calculateFractionAndDistance(originalGeometry,
                            isoLabelWayGeometry.getEndPoint().getCoordinate())
                    .getFraction();
        }

        return IsochroneMatch.builder()
                .matchedLinkId(roadSectionId)
                // Rounding here to avoid near zero fraction
                .startFraction(BigDecimal.valueOf(startFraction)
                        .setScale(ROUNDING_DECIMAL_PLACES, RoundingMode.HALF_UP)
                        .doubleValue())
                // Rounding here to avoid near one fraction
                .endFraction(BigDecimal.valueOf(endFraction)
                        .setScale(ROUNDING_DECIMAL_PLACES, RoundingMode.HALF_UP)
                        .doubleValue())
                .reversed(reversed)
                .geometry(isoLabelWayGeometry)
                .build();
    }

    public boolean isStartSegment(int roadSectionId, Snap startSegment) {
        IntEncodedValue idEnc = encodingManager.getIntEncodedValue(WAY_ID.getKey());
        int startSegmentId = startSegment.getClosestEdge().get(idEnc);
        return roadSectionId == startSegmentId;
    }

    private LineString getStartSegmentWayGeometryInTravelDirection(boolean reversed, Snap startSegment) {
        LineString startSegmentWayGeometry = startSegment
                .getClosestEdge().fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION);
        LineString startSegmentWayGeometryInForwardDirection =
                edgeIteratorStateReverseExtractor.hasReversed(startSegment.getClosestEdge())
                        ? startSegmentWayGeometry.reverse()
                        : startSegmentWayGeometry;
        return reversed ? startSegmentWayGeometryInForwardDirection.reverse() :
                startSegmentWayGeometryInForwardDirection;
    }

    private LineString calculatePartialGeometry(LineString edgeGeometry, double isoLabelEdgeGeometryDistance,
            double totalDistanceTravelled, double maxDistance) {
        double distanceToEndOfEdge = isoLabelEdgeGeometryDistance - (maxDistance - (totalDistanceTravelled
                - isoLabelEdgeGeometryDistance));
        double partialFraction = (isoLabelEdgeGeometryDistance - distanceToEndOfEdge)
                / isoLabelEdgeGeometryDistance;
        return getSubLineString(edgeGeometry, partialFraction);
    }

    /**
     * Extraction of a sub-LineString from an existing line, starting from 0; The line is converted to rd-new to get a
     * more precise result in meters and then converted back to wgs-84
     *
     * @param ls       the line from which we extract the sub LineString ()
     * @param fraction [0..1], the length until where we want the substring to go
     * @return the sub-LineString
     */
    private LineString getSubLineString(LineString ls, double fraction) {
        if (fraction >= 1) {
            return ls;
        }
        Geometry rdGeom = crsTransformer.transformFromWgs84ToRdNew(ls);
        LengthIndexedLine linRefLine = new LengthIndexedLine(rdGeom);
        return (LineString) crsTransformer
                .transformFromRdNewToWgs84(linRefLine.extractLine(0, fraction * rdGeom.getLength()));
    }
}
