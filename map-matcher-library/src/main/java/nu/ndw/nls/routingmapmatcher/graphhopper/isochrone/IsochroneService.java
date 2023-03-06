package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;


import static java.util.Comparator.comparing;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
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

@RequiredArgsConstructor
public class IsochroneService {

    private static final int ROOT_PARENT = -1;
    private static final int METERS = 1000;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int MILLISECONDS = 1000;
    private final LinkFlagEncoder flagEncoder;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final IsochroneMatchMapper isochroneMatchMapper;
    private final IsochroneFactory isochroneFactory;

    /**
     * Performs an up-stream isochrone search and returns a list of isochrone matches containing exact cropped
     * geometries with start and end fractions.
     *
     * @param matchedPoint      The nearest point found by the nearest match from which to start the isochrone search
     * @param queryGraph        The query graph to use in the isochrone search
     * @param location          Base location containing isochrone specifications
     * @param locationIndexTree The spatial index to retrieve the start segment from
     * @return A list of isochrone matches with the geometries cropped to the max distance. The geometry is aligned in
     * the direction of travelling. Start and en fraction are with respect to this alignment (positive negative)
     */
    public List<IsochroneMatch> getUpstreamIsochroneMatches(MatchedPoint matchedPoint,
            QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint, queryGraph, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), locationIndexTree, true);
    }

    /**
     * Performs a down-stream isochrone search and returns a list of isochrone matches containing exact cropped
     * geometries with start and end fractions.
     *
     * @param matchedPoint      The nearest point found by the nearest match from which to start the isochrone search
     * @param queryGraph        The query graph to use in the isochrone search
     * @param location          Base location containing isochrone specifications
     * @param locationIndexTree The spatial index to retrieve the start segment from
     * @return A list of isochrone matches with the geometries cropped to the max distance. The geometry is aligned in
     * the direction of travelling. Start and en fraction are with respect to this alignment (positive negative)
     */
    public List<IsochroneMatch> getDownstreamIsochroneMatches(MatchedPoint matchedPoint,
            QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint, queryGraph, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), locationIndexTree, false);
    }

    public Set<Integer> getUpstreamLinkIds(QueryGraph queryGraph, BaseLocation location, int nodeId) {
        return getIsochroneLinkIds(queryGraph, true, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), nodeId);
    }

    public Set<Integer> getDownstreamLinkIds(QueryGraph queryGraph, BaseLocation location, int nodeId) {
        return getIsochroneLinkIds(queryGraph, false, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), nodeId);
    }

    private List<IsochroneMatch> getIsochroneMatches(MatchedPoint matchedPoint,
            QueryGraph queryGraph,
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

        /*
            Lookup will create virtual edges based on the snapped point, thereby cutting the segment in 2 line strings.
            It also sets the closestNode of the matchedQueryResult to the virtual node id. In this way it creates a
            start point for isochrone calculation based on the snapped point coordinates.
        */

        queryGraph.lookup(List.of(startSegment));
        Isochrone isochrone = isochroneFactory.createIsochrone(queryGraph, isochroneValue, isochroneUnit, reverseFlow);
        // Here the ClosestNode is the virtual node id created by the queryGraph.lookup.
        List<IsoLabel> labels = isochrone.search(startSegment.getClosestNode());
        // down stream false upstream true
        boolean searchDirectionReversed = !reverseFlow && matchedPoint.isReversed();
        return labels.stream()
                /*
                    With bidirectional start segments the search goes two ways for both down and upstream isochrones.
                    The  branches that are starting in the wrong direction of travelling
                    (as determined by the nearest match) are filtered out.
                */
                .filter(isoLabel -> isSegmentFromStartSegmentInCorrectDirection(searchDirectionReversed,
                        isoLabel,
                        startSegment,
                        queryGraph))
                .sorted(comparing(isoLabel -> isoLabel.distance))
                .map(isoLabel -> {
                      /*
                            Specify the maximum distance on which to crop the geometries use meters
                            or accumulate dynamically based on the average speed of the iso-label in case of seconds.
                       */
                    double maxDistance = IsochroneUnit.METERS == isochroneUnit ? isochroneValue
                            : calculateMaxDistance(queryGraph, isochroneValue, 0, isoLabel);
                    return isochroneMatchMapper.mapToIsochroneMatch(isoLabel, maxDistance, queryGraph, startSegment);
                })
                .collect(Collectors.toList());
    }

    /**
     * This method recursively calculates the max distance based on the time it takes to traverse an entire branch of
     * road sections. It takes the encoded average speed of each traversed road section. Used for time based isochrone
     * searches.
     *
     * @param queryGraph           the query graph to get the average speed from the edge
     * @param maximumTimeInSeconds the maximum time in seconds
     * @param maxDistance          the accumulated distance
     * @param isoLabel             the isoLabel
     */
    private double calculateMaxDistance(QueryGraph queryGraph, double maximumTimeInSeconds,
            double maxDistance,
            IsoLabel isoLabel) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge,
                isoLabel.adjNode);
        double averageSpeed = currentEdge.get(flagEncoder.getAverageSpeedEnc());
        double distanceDeltaInMeters =
                isoLabel.distance - ((IsoLabel) isoLabel.parent).distance;
        double timeToTraverseInSeconds =
                distanceDeltaInMeters / (averageSpeed * METERS / SECONDS_PER_HOUR);
        double totalTime = (double) isoLabel.time / MILLISECONDS;
        if (totalTime <= maximumTimeInSeconds) {
            maxDistance +=
                    timeToTraverseInSeconds * (averageSpeed * METERS / SECONDS_PER_HOUR);
        } else {
            maxDistance +=
                    (maximumTimeInSeconds - (totalTime - timeToTraverseInSeconds)) * (
                            averageSpeed * METERS / SECONDS_PER_HOUR);
        }
        // Traverse back to the start segment
        if (isoLabel.parent.edge != ROOT_PARENT) {
            return calculateMaxDistance(queryGraph, maximumTimeInSeconds, maxDistance, (IsoLabel) isoLabel.parent);
        }
        return maxDistance;
    }


    private Set<Integer> getIsochroneLinkIds(QueryGraph queryGraph, boolean reverse, double isochroneValue,
            IsochroneUnit isochroneUnit, int nodeId) {
        Isochrone isochrone = isochroneFactory.createIsochrone(queryGraph, isochroneValue, isochroneUnit, reverse);
        List<Isochrone.IsoLabel> labels = isochrone.search(nodeId);
        return labels.stream()
                .map(l -> queryGraph.getEdgeIteratorState(l.edge, l.adjNode))
                .map(EdgeIteratorState::getFlags)
                .map(flagEncoder::getId)
                .collect(Collectors.toSet());
    }


    /**
     * This method recursively goes through the parent list to find the start segment. It then determines if the start
     * segment is in the correct direction for the upstream or downstream query.
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
        } else {
            boolean isInCorrectDirection = true;
            EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
            int roadSectionId = flagEncoder.getId(currentEdge.getFlags());
            if (isochroneMatchMapper.isStartSegment(roadSectionId, startSegment)) {
                isInCorrectDirection = edgeIteratorStateReverseExtractor.hasReversed(currentEdge) == reverse;
            }
            if (isoLabel.parent.edge != ROOT_PARENT) {
                return isSegmentFromStartSegmentInCorrectDirection(reverse, isoLabel.parent, startSegment, queryGraph);
            }
            return isInCorrectDirection;
        }
    }
}
