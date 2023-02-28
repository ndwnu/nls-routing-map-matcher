package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;

import static com.graphhopper.storage.EdgeIteratorStateReverseExtractor.hasReversed;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
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
    private final CrsTransformer crsTransformer;
    private final QueryResult startSegment;
    private final QueryGraph queryGraph;
    private final LinkFlagEncoder flagEncoder;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final double maxDistance;

    public IsochroneMatch mapToIsochroneMatch(IsoLabel isoLabel) {
        var currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
        var edgeDirection = hasReversed(currentEdge) ? Direction.BACKWARD : Direction.FORWARD;
        var averageSpeed = currentEdge.get(flagEncoder.getAverageSpeedEnc());
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
                .getDistance();

        /*
         *   The start segment in the iso-label is split into 2 sections sometimes in opposite directions
         *   in case of bidirectional roads.
         *   Here the fractions are calculated based on the entire start-segment geometry.
         * */
        if (isStartSegment(currentEdge, startSegment)) {
            // If the total distance travelled exceeds the maximum distance cut the linestring
            if (totalDistanceTravelled > maxDistance) {
                isoLabelEdgeGeometry = calculatePartialGeometry(isoLabelEdgeGeometry,
                        isoLabelEdgeGeometryDistance, totalDistanceTravelled,
                        maxDistance);
            }
            var totalStartSegmentGeometry = startSegment
                    .getClosestEdge().fetchWayGeometry(ALL_NODES)
                    .toLineString(INCLUDE_ELEVATION);
            var totalStartSegmentGeometryInForwardDirection =
                    hasReversed(startSegment.getClosestEdge()) ? totalStartSegmentGeometry.reverse()
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

            // If the total distance travelled exceeds the maximum distance cut the linestring
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
                .startFraction(startFraction)
                .endFraction(endFraction)
                .direction(edgeDirection)
                .geometry(isoLabelEdgeGeometry)
                .build();

    }

    private LineString calculatePartialGeometry(LineString edgeGeometry, double isoLabelEdgeGeometryDistance,
            double totalDistanceTravelled, double maxDistance) {
        var diff = isoLabelEdgeGeometryDistance - (maxDistance - (totalDistanceTravelled
                - isoLabelEdgeGeometryDistance));
        var partialFraction = (isoLabelEdgeGeometryDistance - diff)
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
