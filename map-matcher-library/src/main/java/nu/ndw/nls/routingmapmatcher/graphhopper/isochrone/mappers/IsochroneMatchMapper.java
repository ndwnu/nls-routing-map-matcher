package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;


import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Builder;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch.Direction;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

@Builder
public class IsochroneMatchMapper {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int ALL_NODES = 3;
    private static final int ROUNDING_DECIMAL_PLACES = 12;
    private final CrsTransformer crsTransformer;
    private final LinkFlagEncoder flagEncoder;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;

    /**
     * Maps an IsoLabel to an IsochroneMatch with cropped geometries aligned to travelling direction and respective
     * start and end fractions.
     * @param isoLabel the iso label to map
     * @return an instance of IsochroneMatch
     */
    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel,
            double maxDistance,
            QueryGraph queryGraph, QueryResult startSegment) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
        /*  Here the reversed boolean indicates the direction of travelling along the edge
         *  with respect to the original alignment of the geometry
         * (can be backward for bidirectional edges or for upstream isochrone searches).
         */
        Direction edgeDirection = edgeIteratorStateReverseExtractor
                .hasReversed(currentEdge) ? Direction.BACKWARD : Direction.FORWARD;
        int roadSectionId = flagEncoder.getId(currentEdge.getFlags());
        double totalDistanceTravelled = isoLabel.distance;
        double startFraction = 0D;
        double endFraction = 1D;
        // This is the entire way geometry except for the start segment which is split up at the start point
        LineString isoLabelWayGeometry = currentEdge
                .fetchWayGeometry(ALL_NODES)
                .toLineString(INCLUDE_ELEVATION);
        double isoLabelEdgeGeometryDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                        isoLabelWayGeometry,
                        isoLabelWayGeometry.getStartPoint().getCoordinate())
                .getTotalDistance();

        /*
         *   The start segment in the iso-label is split into 2 sections, in case of bidirectional roads
         *   in opposite directions as indicated by the edgeDirection.
         *   Here the fractions are calculated based on the entire start-segment geometry
         *   with respect to the partial edges.
         * */
        if (isStartSegment(roadSectionId, startSegment)) {
            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
            if (totalDistanceTravelled > maxDistance) {
                isoLabelWayGeometry = calculatePartialGeometry(isoLabelWayGeometry,
                        isoLabelEdgeGeometryDistance, totalDistanceTravelled,
                        maxDistance);
            }
            LineString startSegmentWayGeometryInTravelDirection = getStartSegmentWayGeometryInTravelDirection(
                    edgeDirection, startSegment);

            startFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            startSegmentWayGeometryInTravelDirection,
                            isoLabelWayGeometry.getStartPoint().
                                    getCoordinate())
                    .getFraction();
            endFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            startSegmentWayGeometryInTravelDirection,
                            isoLabelWayGeometry.getEndPoint().
                                    getCoordinate())
                    .getFraction();
            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
        } else if (totalDistanceTravelled > maxDistance) {
            LineString originalGeometry = (LineString) isoLabelWayGeometry.copy();
            isoLabelWayGeometry = calculatePartialGeometry(isoLabelWayGeometry,
                    isoLabelEdgeGeometryDistance, totalDistanceTravelled,
                    maxDistance);

            endFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            originalGeometry,
                            isoLabelWayGeometry.getEndPoint().
                                    getCoordinate())
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
                .direction(edgeDirection)
                .geometry(isoLabelWayGeometry)
                .build();

    }

    private LineString getStartSegmentWayGeometryInTravelDirection(Direction edgeDirection, QueryResult startSegment) {
        LineString startSegmentWayGeometry = startSegment
                .getClosestEdge().fetchWayGeometry(ALL_NODES)
                .toLineString(INCLUDE_ELEVATION);
        LineString startSegmentWayGeometryInForwardDirection =
                edgeIteratorStateReverseExtractor.hasReversed(startSegment.getClosestEdge())
                        ? startSegmentWayGeometry.reverse()
                        : startSegmentWayGeometry;
        return edgeDirection == Direction.FORWARD ?
                startSegmentWayGeometryInForwardDirection :
                startSegmentWayGeometryInForwardDirection.reverse();
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
     * Extraction of a sub-LineString from an existing line, starting from 0;
     * The line is converted to rd-new to get a more precise result in meters and then converted back to wgs-84
     * @param ls       the line from which we extract the sub LineString ()
     * @param fraction [0..1], the length until where we want the substring to go
     * @return the sub-LineString
     */
    private LineString getSubLineString(LineString ls, double fraction) {
        if (fraction >= 1) {
            return ls;
        }
        var rdGeom = crsTransformer.transformFromWgs84ToRdNew(ls);
        LengthIndexedLine linRefLine = new LengthIndexedLine(rdGeom);
        return (LineString) crsTransformer
                .transformFromRdNewToWgs84(linRefLine.extractLine(0, fraction * rdGeom.getLength()));
    }

    private boolean isStartSegment(int roadSectionId, QueryResult startSegment) {
        var startSegmentId = flagEncoder.getId(startSegment.getClosestEdge().getFlags());
        return roadSectionId == startSegmentId;
    }
}
