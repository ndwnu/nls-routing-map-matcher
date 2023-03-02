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
    private final QueryResult startSegment;
    private final QueryGraph queryGraph;
    private final LinkFlagEncoder flagEncoder;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final double maxDistance;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;

    /**
     * Maps an IsoLabel to an IsochroneMatch with cropped geometries aligned to travelling direction and respective
     * start and end fractions.
     *
     * @param isoLabel the iso label to map
     * @return an instance of IsochroneMatch
     */
    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel) {
        var currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
        /*  Here the reversed boolean indicates the direction of travelling along the edge
         *  with respect to the original alignment of the geometry
         * (can be backward for bidirectional edges or for upstream isochrone searches).
         */
        var edgeDirection = edgeIteratorStateReverseExtractor
                .hasReversed(currentEdge) ? Direction.BACKWARD : Direction.FORWARD;
        var edgeFlags = currentEdge.getFlags();
        var roadSectionId = flagEncoder.getId(edgeFlags);
        var totalDistanceTravelled = isoLabel.distance;
        var startFraction = 0D;
        var endFraction = 1D;
        // This is the entire way geometry except for the start segment which is split up at the start point
        var isoLabelEdgeGeometry = currentEdge
                .fetchWayGeometry(ALL_NODES)
                .toLineString(INCLUDE_ELEVATION);
        var isoLabelEdgeGeometryDistance = fractionAndDistanceCalculator.calculateFractionAndDistance(
                        isoLabelEdgeGeometry,
                        isoLabelEdgeGeometry.getStartPoint().getCoordinate())
                .getTotalDistance();

        /*
         *   The start segment in the iso-label is split into 2 sections, in case of bidirectional roads
         *   in opposite directions as indicated by the edgeDirection.
         *   Here the fractions are calculated based on the entire start-segment geometry.
         * */
        if (isStartSegment(currentEdge, startSegment)) {
            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
            if (totalDistanceTravelled > maxDistance) {
                isoLabelEdgeGeometry = calculatePartialGeometry(isoLabelEdgeGeometry,
                        isoLabelEdgeGeometryDistance, totalDistanceTravelled,
                        maxDistance);
            }
            var totalStartSegmentGeometry = startSegment
                    .getClosestEdge().fetchWayGeometry(ALL_NODES)
                    .toLineString(INCLUDE_ELEVATION);
            var totalStartSegmentGeometryInForwardDirection =
                    edgeIteratorStateReverseExtractor.hasReversed(startSegment.getClosestEdge())
                            ? totalStartSegmentGeometry.reverse()
                            : totalStartSegmentGeometry;
            var totalStartSegmentGeometryInDirectionOfTravelling = edgeDirection == Direction.FORWARD ?
                    totalStartSegmentGeometryInForwardDirection :
                    totalStartSegmentGeometryInForwardDirection.reverse();
            endFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            totalStartSegmentGeometryInDirectionOfTravelling,
                            isoLabelEdgeGeometry.getEndPoint().
                                    getCoordinate())
                    .getFraction();
            startFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            totalStartSegmentGeometryInDirectionOfTravelling,
                            isoLabelEdgeGeometry.getStartPoint().
                                    getCoordinate())
                    .getFraction();

            // If the total distance travelled exceeds the maximum distance cut the linestring accordingly.
        } else if (totalDistanceTravelled > maxDistance) {
            isoLabelEdgeGeometry = calculatePartialGeometry(isoLabelEdgeGeometry,
                    isoLabelEdgeGeometryDistance, totalDistanceTravelled,
                    maxDistance);
            startFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            isoLabelEdgeGeometry,
                            isoLabelEdgeGeometry.getStartPoint().
                                    getCoordinate())
                    .getFraction();
            endFraction = fractionAndDistanceCalculator.calculateFractionAndDistance(
                            isoLabelEdgeGeometry,
                            isoLabelEdgeGeometry.getEndPoint().
                                    getCoordinate())
                    .getFraction();

        }
        return IsochroneMatch.builder()
                .matchedLinkId(roadSectionId)
                .startFraction(new BigDecimal(startFraction)
                        .setScale(ROUNDING_DECIMAL_PLACES, RoundingMode.HALF_UP)
                        .doubleValue())
                .endFraction(new BigDecimal(endFraction)
                        .setScale(ROUNDING_DECIMAL_PLACES, RoundingMode.HALF_UP)
                        .doubleValue())
                .direction(edgeDirection)
                .geometry(isoLabelEdgeGeometry)
                .build();

    }

    private LineString calculatePartialGeometry(LineString edgeGeometry, double isoLabelEdgeGeometryDistance,
            double totalDistanceTravelled, double maxDistance) {
        var remainingDistanceOnEdge = isoLabelEdgeGeometryDistance - (maxDistance - (totalDistanceTravelled
                - isoLabelEdgeGeometryDistance));
        var partialFraction = (isoLabelEdgeGeometryDistance - remainingDistanceOnEdge)
                / isoLabelEdgeGeometryDistance;
        return getSubLineString(edgeGeometry, partialFraction);

    }

    /**
     * Extraction of a sub-LineString from an existing line, starting from 0;
     *
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

    private boolean isStartSegment(EdgeIteratorState edgeIteratorState, QueryResult startSegment) {
        var flags = edgeIteratorState.getFlags();
        var id = flagEncoder.getId(flags);
        var startSegmentId = flagEncoder.getId(startSegment.getClosestEdge().getFlags());
        return id == startSegmentId;
    }
}
