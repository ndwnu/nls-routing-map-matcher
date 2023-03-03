package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;


import static java.util.Comparator.comparing;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.geotools.referencing.GeodeticCalculator;

@RequiredArgsConstructor
public class IsochroneService {

    private static final int ROOT_PARENT = -1;
    private static final int METERS = 1000;
    private static final int SECONDS_PER_HOUR = 3600;
    private final LinkFlagEncoder flagEncoder;
    private final Weighting weighting;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;

    /**
     * Performs an isochrone search and returns a list of isochrone matches containing exact cropped geometries with
     * start and end fractions.
     *
     * @param matchedPoint      The nearest point found by the nearest match from which to start the isochrone search
     * @param queryGraph        The query graph to use in the isochrone search
     * @param location          Base location containing isochrone specifications
     * @param locationIndexTree The spatial index to retrieve the start segment from
     * @return A list of isochrone matches with the geometries cropped to the max distance. The geometry is aligned in
     * the direction of travelling. Start and en fraction are with respect to this alignment (positive negative)
     */
    public List<IsochroneMatch> getUpstreamIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint, queryGraph, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), locationIndexTree, true);
    }

    public List<IsochroneMatch> getDownstreamIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint, queryGraph, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), locationIndexTree, false);
    }

    private List<IsochroneMatch> getIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            double isochroneValue,
            IsochroneUnit isochroneUnit,
            LocationIndexTree locationIndexTree,
            boolean reverseFlow) {
        double latitude = matchedPoint.getSnappedPoint().getY();
        double longitude = matchedPoint.getSnappedPoint().getX();
        // Get the  start segment for the isochrone calculation
        QueryResult startSegment
                = locationIndexTree
                .findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);
        /* Lookup will create virtual edges based on the snapped point, thereby cutting the segment in 2 line strings.
           It also sets the closestNode of the matchedQueryResult to the virtual node id. In this way it creates a
           start point for isochrone calculation based on the snapped point coordinates.
        **/
        queryGraph.lookup(List.of(startSegment));
        double averageSpeed = startSegment.getClosestEdge().get(flagEncoder.getAverageSpeedEnc());
        /*  Specify the maximum distance on which to crop the geometries use meters
            or seconds * meters per second from averageSpeed info of start segment
        **/

        Isochrone isochrone = configureIsochrone(queryGraph, isochroneValue, isochroneUnit, reverseFlow);
        // Here the ClosestNode is the virtual node id created by the queryGraph.lookup.
        List<IsoLabel> labels = isochrone.search(startSegment.getClosestNode());
        double maxDistance =
                IsochroneUnit.METERS == isochroneUnit ? isochroneValue :
                        (isochroneValue * (averageSpeed * METERS / SECONDS_PER_HOUR));
        IsochroneMatchMapper isoLabelMapper = IsochroneMatchMapper
                .builder()
                .crsTransformer(new CrsTransformer())
                .fractionAndDistanceCalculator(new FractionAndDistanceCalculator(new GeodeticCalculator()))
                .maxDistance(maxDistance)
                .startSegment(startSegment)
                .flagEncoder(flagEncoder)
                .queryGraph(queryGraph)
                .edgeIteratorStateReverseExtractor(edgeIteratorStateReverseExtractor)
                .build();
        // down stream false upstream true
        boolean searchDirectionReversed = !reverseFlow && matchedPoint.isReversed();
        return labels.stream()
                // With bidirectional start segments the search goes two ways for both down and upstream isochrones.
                // The  branches that are starting in the wrong direction of travelling
                // (as determined by the nearest match) are filtered out.
                .filter(isoLabel -> isSegmentFromStartSegmentInCorrectDirection(searchDirectionReversed,
                        isoLabel,
                        startSegment,
                        queryGraph))
                .sorted(comparing(isoLabel -> isoLabel.distance))
                .map(isoLabelMapper::mapToIsochroneMatch)
                .collect(Collectors.toList());
    }

    private Isochrone configureIsochrone(QueryGraph queryGraph, double isochroneValue, IsochroneUnit isochroneUnit,
            boolean reverseFlow) {
        Isochrone isochrone = new Isochrone(queryGraph, this.weighting, reverseFlow);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
        return isochrone;
    }


    public Set<Integer> getUpstreamLinkIds(QueryGraph queryGraph, BaseLocation location, int nodeId) {
        return getIsochroneLinkIds(queryGraph, true, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), nodeId);
    }

    public Set<Integer> getDownstreamLinkIds(QueryGraph queryGraph, BaseLocation location, int nodeId) {
        return getIsochroneLinkIds(queryGraph, false, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), nodeId);
    }

    private Set<Integer> getIsochroneLinkIds(QueryGraph queryGraph, boolean reverse, double isochroneValue,
            IsochroneUnit isochroneUnit, int nodeId) {
        Isochrone isochrone = configureIsochrone(queryGraph, isochroneValue, isochroneUnit, reverse);
        List<Isochrone.IsoLabel> labels = isochrone.search(nodeId);
        return labels.stream()
                .map(l -> queryGraph.getEdgeIteratorState(l.edge, l.adjNode))
                .map(EdgeIteratorState::getFlags)
                .map(flagEncoder::getId)
                .collect(Collectors.toSet());
    }

    private boolean isStartSegment(EdgeIteratorState edgeIteratorState, QueryResult startSegment) {
        var flags = edgeIteratorState.getFlags();
        var id = flagEncoder.getId(flags);
        var startSegmentId = flagEncoder.getId(startSegment.getClosestEdge().getFlags());
        return id == startSegmentId;
    }

    /**
     * This method recursively goes through the parent list to find the start segment. It then determines if the start
     * segment is in the correct direction for the upstream or downstream query. The line is converted to rd-new to get
     * a more precise result in meters and then converted back to wgs-84
     *
     * @param reverse      Indicating the correct direction
     * @param isoLabel     The label to be checked
     * @param startSegment Start segment to find
     * @param queryGraph   the context from which to get the currentEdge
     * @return boolean indicating the correct direction
     */
    private boolean isSegmentFromStartSegmentInCorrectDirection(boolean reverse, SPTEntry isoLabel,
            QueryResult startSegment, QueryGraph queryGraph) {

        // If the start segment not bidirectional the search returns the right results
        EdgeIteratorTravelDirection edgeIteratorTravelDirection = determineEdgeDirection(startSegment, flagEncoder);
        if (EdgeIteratorTravelDirection.BOTH_DIRECTIONS != edgeIteratorTravelDirection) {
            return true;
        }
        var currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
        if (isStartSegment(currentEdge, startSegment)) {
            return edgeIteratorStateReverseExtractor.hasReversed(currentEdge) == reverse;
        }
        if (isoLabel.parent.edge != ROOT_PARENT) {
            return isSegmentFromStartSegmentInCorrectDirection(reverse, isoLabel.parent, startSegment, queryGraph);
        }
        return true;
    }
}
