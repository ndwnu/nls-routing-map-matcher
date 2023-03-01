package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import static com.graphhopper.storage.EdgeIteratorStateReverseExtractor.hasReversed;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.geotools.referencing.GeodeticCalculator;

@RequiredArgsConstructor
public class IsochroneService {

    private static final int ROOT_PARENT = -1;
    private final LinkFlagEncoder flagEncoder;
    private final Weighting weighting;


    public List<IsochroneMatch> getUpstreamIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint,queryGraph,location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(),locationIndexTree,true);
    }

    public List<IsochroneMatch> getDownstreamIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            BaseLocation location,
            LocationIndexTree locationIndexTree) {
        return getIsochroneMatches(matchedPoint,queryGraph,location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(),locationIndexTree,false);
    }

    private List<IsochroneMatch> getIsochroneMatches(MatchedPoint matchedPoint,
            final QueryGraph queryGraph,
            double isochroneValue,
            IsochroneUnit isochroneUnit,
            LocationIndexTree locationIndexTree,
            boolean reverseFlow) {
        final double latitude = matchedPoint.getSnappedPoint().getY();
        final double longitude = matchedPoint.getSnappedPoint().getX();
        // Get the  start segment for the isochrone calculation
        var startSegment
                = locationIndexTree
                .findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);
        /* Lookup will create virtual edges based on the snapped cutting the segment in 2 line strings.
           It also sets the closestNode of the matchedQueryResult to the virtual node id creating a
           start point for isochrone calculation the based on the snapped point coordinates.
        **/
        queryGraph.lookup(List.of(startSegment));
        // down stream false upstream true
        var searchDirectionReversed = !reverseFlow && matchedPoint.isReversed();

        var averageSpeed = startSegment.getClosestEdge().get(flagEncoder.getAverageSpeedEnc());
        /*  Specify the maximum distance on which to crop the geometries use meters
            or seconds * meters per second from averageSpeed info of start segment
        **/
        var maxDistance =
                IsochroneUnit.METERS == isochroneUnit ? isochroneValue : isochroneValue * (averageSpeed * 1000 / 3600);
        var isochrone = new Isochrone(queryGraph, this.weighting, reverseFlow);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
        // Here the ClosestNode is the virtual node id created by the queryGraph.lookup.
        var labels = isochrone.search(startSegment.getClosestNode());
        var isoLabelMapper = IsochroneMatchMapper
                .builder()
                .crsTransformer(new CrsTransformer())
                .fractionAndDistanceCalculator(new FractionAndDistanceCalculator(new GeodeticCalculator()))
                .maxDistance(maxDistance)
                .startSegment(startSegment)
                .flagEncoder(flagEncoder)
                .queryGraph(queryGraph)
                .build();
        return labels.stream()
                // With bidirectional start segments the search goes two ways for both down and upstream isochrones.
                // The  branches that are starting in the wrong direction of travelling
                // (as determined by the nearest match) are filtered out.
                .filter(isoLabel -> segmentIsFromStartSegmentInCorrectDirection(searchDirectionReversed,
                        isoLabel,
                        startSegment,
                        queryGraph))
                .map(isoLabelMapper::mapToIsochroneMatch)
                .collect(Collectors.toList());
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
        Isochrone isochrone = new Isochrone(queryGraph, this.weighting, reverse);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
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
     * segment is in the correct direction for the upstream or downstream query.
     *
     * @param reverse      Indicating the correct direction
     * @param isoLabel     The label to be checked
     * @param startSegment Start segment to find
     * @param queryGraph   the context from which to get the currentEdge
     * @return boolean indicating the correct direction
     */
    private boolean segmentIsFromStartSegmentInCorrectDirection(boolean reverse, SPTEntry isoLabel,
            QueryResult startSegment, QueryGraph queryGraph) {
        var currentEdge = queryGraph.getEdgeIteratorState(isoLabel.edge, isoLabel.adjNode);
        // If the start segment not bidirectional the search returns the right results
        EdgeIteratorTravelDirection edgeIteratorTravelDirection = determineEdgeDirection(startSegment, flagEncoder);
        if (EdgeIteratorTravelDirection.BOTH_DIRECTIONS != edgeIteratorTravelDirection) {
            return true;
        }
        if (isStartSegment(currentEdge, startSegment)) {
            return hasReversed(currentEdge) == reverse;
        }
        //
        if (isoLabel.parent.edge != ROOT_PARENT) {
            return segmentIsFromStartSegmentInCorrectDirection(reverse, isoLabel.parent, startSegment, queryGraph);
        }
        return true;
    }
}
