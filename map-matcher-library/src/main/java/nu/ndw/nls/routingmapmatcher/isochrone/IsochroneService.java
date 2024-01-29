package nu.ndw.nls.routingmapmatcher.isochrone;


import static java.util.Comparator.comparing;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static nu.ndw.nls.routingmapmatcher.util.PathUtil.determineEdgeDirection;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsochroneByTimeDistanceAndWeight;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.base.BaseLocation;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
public class IsochroneService {

    private static final int ROOT_PARENT = -1;
    private static final int METERS = 1000;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int MILLISECONDS = 1000;
    private final EncodingManager encodingManager;
    private final BaseGraph baseGraph;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final IsochroneMatchMapper isochroneMatchMapper;
    private final ShortestPathTreeFactory shortestPathTreeFactory;
    private final LocationIndexTree locationIndexTree;
    private final Profile profile;

    /**
     * Performs an up-stream isochrone search and returns a list of isochrone matches containing exact cropped
     * geometries with start and end fractions.
     *
     * @param startPoint The start point from which to start the isochrone search
     * @param reversed   Boolean indicating reversed direction of travelling with respect to the original geometry
     * @param location   Base location containing isochrone specifications
     * @return A list of isochrone matches with the geometries cropped to the max distance. The geometry is aligned in
     * the direction of travelling. Start and en fraction are with respect to this alignment (positive negative)
     */
    public List<IsochroneMatch> getUpstreamIsochroneMatches(Point startPoint, boolean reversed,
            BaseLocation location) {
        return getIsochroneMatches(startPoint, reversed, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), true);
    }

    /**
     * Performs a down-stream isochrone search and returns a list of isochrone matches containing exact cropped
     * geometries with start and end fractions.
     *
     * @param startPoint The start point from which to start the isochrone search
     * @param reversed   Boolean indicating reversed direction of travelling with respect to the original geometry
     * @param location   Base location containing isochrone specifications
     * @return A list of isochrone matches with the geometries cropped to the max distance. The geometry is aligned in
     * the direction of travelling. Start and en fraction are with respect to this alignment (positive negative)
     */
    public List<IsochroneMatch> getDownstreamIsochroneMatches(Point startPoint, boolean reversed,
            BaseLocation location) {
        return getIsochroneMatches(startPoint, reversed, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), false);
    }

    private List<IsochroneMatch> getIsochroneMatches(Point startPoint,
            boolean reversed,
            double isochroneValue,
            IsochroneUnit isochroneUnit,

            boolean reverseFlow) {
        double latitude = startPoint.getY();
        double longitude = startPoint.getX();
        // Get the  start segment for the isochrone calculation
        Snap startSegment
                = locationIndexTree
                .findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);

        /*
            Lookup will create virtual edges based on the snapped point, thereby cutting the segment in 2 line strings.
            It also sets the closestNode of the matchedQueryResult to the virtual node id. In this way it creates a
            start point for isochrone calculation based on the snapped point coordinates.
        */

        QueryGraph queryGraph = QueryGraph.create(baseGraph, startSegment);
        IsochroneByTimeDistanceAndWeight isochrone = shortestPathTreeFactory
                .createShortestPathTreeByTimeDistanceAndWeight(
                null, queryGraph,
                TraversalMode.NODE_BASED,
                isochroneValue,
                isochroneUnit, reverseFlow);
        // Here the ClosestNode is the virtual node id created by the queryGraph.lookup.
        List<IsoLabel> isoLabels = new ArrayList<>();
        isochrone.search(startSegment.getClosestNode(), isoLabels::add);
        boolean searchDirectionReversed = reversed != reverseFlow;
        return isoLabels.stream()
                .filter(isoLabel -> isoLabel.getEdge() != ROOT_PARENT)
                /*
                    With bidirectional start segments the search goes two ways for both down and upstream isochrones.
                    The  branches that are starting in the wrong direction of travelling
                    (as determined by the nearest match) are filtered out.
                */
                .filter(isoLabel -> isSegmentFromStartSegmentInCorrectDirection(searchDirectionReversed,
                        isoLabel,
                        startSegment,
                        queryGraph))
                .sorted(comparing(IsoLabel::getDistance))
                .map(isoLabel -> {
                      /*
                            Specify the maximum distance on which to crop the geometries use meters
                            or accumulate dynamically based on the average speed of the iso-label in case of seconds.
                       */
                    double maxDistance = IsochroneUnit.METERS == isochroneUnit ? isochroneValue
                            : calculateMaxDistance(queryGraph, isochroneValue, isoLabel, searchDirectionReversed);
                    return isochroneMatchMapper.mapToIsochroneMatch(isoLabel, maxDistance, queryGraph, startSegment);
                })
                .collect(Collectors.toList());
    }

    /**
     * This method calculates the max distance based on the time it takes to traverse an entire branch of road sections.
     * It takes the encoded average speed of the road section. Used for time based isochrone searches.
     *
     * @param queryGraph           the query graph to get the average speed from the edge
     * @param maximumTimeInSeconds the maximum time in seconds
     * @param isoLabel             the isoLabel
     */
    private double calculateMaxDistance(QueryGraph queryGraph, double maximumTimeInSeconds,
            IsoLabel isoLabel, boolean useSpeedFromReversedDirection) {
        EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.getEdge(),
                isoLabel.getNode());
        double averageSpeed = getAverageSpeedFromEdge(currentEdge, useSpeedFromReversedDirection);
        double totalTime = (double) isoLabel.getTime() / MILLISECONDS;
        double maxDistance;
        if (totalTime <= maximumTimeInSeconds) {
            maxDistance = isoLabel.getDistance();
        } else {
            /*  Assuming that the iso label values for distance
                and time are correctly calculated based on the average speed.
                We can then calculate the max distance by subtracting the time difference * metersPerSecond
             */
            double metersPerSecond = averageSpeed * METERS / SECONDS_PER_HOUR;
            maxDistance = isoLabel.getDistance() - ((totalTime - maximumTimeInSeconds) * metersPerSecond);
        }
        return maxDistance;
    }

    private double getAverageSpeedFromEdge(EdgeIteratorState currentEdge, boolean useSpeedFromReversedDirection) {
        DecimalEncodedValue vehicleSpeedDecimalEncodedValue = encodingManager.getDecimalEncodedValue(
                VehicleSpeed.key(profile.getVehicle()));
        if (useSpeedFromReversedDirection) {
            return currentEdge.getReverse(vehicleSpeedDecimalEncodedValue);
        }
        return currentEdge.get(vehicleSpeedDecimalEncodedValue);
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
    private boolean isSegmentFromStartSegmentInCorrectDirection(boolean reverse, IsoLabel isoLabel,
            Snap startSegment, QueryGraph queryGraph) {
        // If the start segment not bidirectional the search returns the right results
        EdgeIteratorTravelDirection edgeIteratorTravelDirection = determineEdgeDirection(startSegment, encodingManager,
                profile.getVehicle());
        if (EdgeIteratorTravelDirection.BOTH_DIRECTIONS != edgeIteratorTravelDirection) {
            return true;
        } else {
            boolean isCorrect = true;
            EdgeIteratorState currentEdge = queryGraph.getEdgeIteratorState(isoLabel.getEdge(), isoLabel.getNode());
            int roadSectionId = currentEdge.get(encodingManager.getIntEncodedValue(WAY_ID_KEY));
            if (isochroneMatchMapper.isStartSegment(roadSectionId, startSegment)) {
                isCorrect = edgeIteratorStateReverseExtractor.hasReversed(currentEdge) == reverse;
            }
            if (isoLabel.getParent().getEdge() != ROOT_PARENT) {
                return isSegmentFromStartSegmentInCorrectDirection(reverse, isoLabel.getParent(), startSegment,
                        queryGraph);
            }
            return isCorrect;
        }


    }
}
