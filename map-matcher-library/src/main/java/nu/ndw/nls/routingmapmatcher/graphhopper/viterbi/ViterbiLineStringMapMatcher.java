package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.CustomDistanceCalc;
import nu.ndw.nls.routingmapmatcher.util.PathUtil;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ViterbiLineStringMapMatcher implements LineStringMapMatcher {

    /**
     * The standard deviation of GPS observations.
     * <p>
     * Only search for candidates within this distance.
     */
    private static final double MEASUREMENT_ERROR_SIGMA_IN_METERS = 20.0;

    /**
     * The beta (1/lambda) parameter used for the exponential distribution to determine the
     * probability that the
     * length of a route between two successive GPS observations is the same as the distance as the
     * crow flies
     * between those GPS observations.
     */
    private static final double TRANSITION_PROBABILITY_BETA = 100.0;

    /**
     * When creating a GPS track, only create GPS "observations" for coordinates that are within this
     * distance of the NDW base network.
     * <p>
     * See also the comment in {@link #createGpsTrack(LineString)}
     */
    private static final double NEARBY_NDW_NETWORK_DISTANCE_IN_METERS = 2 * MEASUREMENT_ERROR_SIGMA_IN_METERS;

    /**
     * Speed to use when creating a GPS track from a geometry.
     * <p>
     * This value does not affect the matching.
     */
    private static final double GPS_TRACK_SPEED_IN_METERS_PER_SECOND = 3.0;

    private static final boolean REDUCE_TO_SEGMENT = true;

    private static final int MILLIS_PER_SECOND = 1000;

    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private static final double DISTANCE_PENALTY_FACTOR = 1.5;
    private static final double PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR = 0.1;
    private static final int NEEDED_GPS_TRACK_ENTRIES = 2;

    private final LinkFlagEncoder flagEncoder;
    private final MapMatching mapMatching;
    private final CustomDistanceCalc distanceCalc;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final PathUtil pathUtil;
    private final QueryGraphExtractor queryGraphExtractor;

    public ViterbiLineStringMapMatcher(final NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        final List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);
        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        final String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.mapMatching = new MapMatching(network, new AlgorithmOptions(algorithm, weighting));
        mapMatching.setMeasurementErrorSigma(MEASUREMENT_ERROR_SIGMA_IN_METERS);
        mapMatching.setTransitionProbabilityBeta(TRANSITION_PROBABILITY_BETA);
        this.distanceCalc = new CustomDistanceCalc();
        mapMatching.setDistanceCalc(distanceCalc);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(geometryFactory);
        this.queryGraphExtractor = new QueryGraphExtractor();
    }

    @Override
    public LineStringMatch match(final LineStringLocation lineStringLocation) {
        Preconditions.checkNotNull(lineStringLocation);

        final List<GPXEntry> gpsTrack = createGpsTrack(lineStringLocation.getGeometry());

        LineStringMatch lineStringMatch;
        if (gpsTrack.size() >= NEEDED_GPS_TRACK_ENTRIES) {
            try {
                preventFilteringWhileMapMatching(gpsTrack);
                final MatchResult matchResult = mapMatching.doWork(gpsTrack);
                if (matchResult.getMergedPath().getEdgeCount() > 0) {
                    lineStringMatch = createMatch(matchResult, lineStringLocation);
                } else {
                    lineStringMatch = createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH);
                }
            } catch (final Exception e) {
                log.debug("Exception while map matching, creating failed result for {}", lineStringLocation, e);
                lineStringMatch = createFailedMatch(lineStringLocation, MatchStatus.EXCEPTION);
            }
        } else {
            lineStringMatch = createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH);
        }
        return lineStringMatch;
    }

    private List<GPXEntry> createGpsTrack(final LineString lineString) {
        final CoordinateSequence coordinateSequence = lineString.getCoordinateSequence();

        final List<GPXEntry> gpsTrack = new ArrayList<>();
        long previousTimestampInMillis = 0;
        for (int index = 0; index < coordinateSequence.size(); index++) {
            final long timestampInMillis;
            if (index == 0) {
                timestampInMillis = previousTimestampInMillis;
            } else {
                final double distanceInMeters = distanceCalc.calcDist(
                        coordinateSequence.getY(index - 1), coordinateSequence.getX(index - 1),
                        coordinateSequence.getY(index), coordinateSequence.getX(index));
                final double durationInSeconds = distanceInMeters / GPS_TRACK_SPEED_IN_METERS_PER_SECOND;
                timestampInMillis = previousTimestampInMillis + Math.round(durationInSeconds * MILLIS_PER_SECOND);
            }
            final GPXEntry gpxEntry = new GPXEntry(coordinateSequence.getY(index), coordinateSequence.getX(index),
                    timestampInMillis);
            // Only add gpx entry when coordinate is nearby the NDW base network. This way, when an empty
            // GPS track is
            // returned, we can be pretty confident that there is no matching possible on the NDW base
            // network.
            if (isNearbyNdwNetwork(gpxEntry)) {
                gpsTrack.add(gpxEntry);
            }
            previousTimestampInMillis = timestampInMillis;
        }
        return gpsTrack;
    }

    private boolean isNearbyNdwNetwork(final GPXEntry gpxEntry) {
        final List<QueryResult> queryResults = locationIndexTree.findNClosest(
                gpxEntry.getLat(), gpxEntry.getLon(), edgeFilter, MEASUREMENT_ERROR_SIGMA_IN_METERS);
        boolean nearbyNdwNetwork = false;
        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= NEARBY_NDW_NETWORK_DISTANCE_IN_METERS) {
                nearbyNdwNetwork = true;
                break;
            }
        }
        return nearbyNdwNetwork;
    }

    private void preventFilteringWhileMapMatching(final List<GPXEntry> gpsTrack) {
        final double customDistance = 3 * MEASUREMENT_ERROR_SIGMA_IN_METERS;
        // When filtering there is no distance calculation for the first and last GPS coordinates
        final int numberOfCalls = Math.max(gpsTrack.size() - 2, 0);
        distanceCalc.returnCustomDistanceForNextCalls(customDistance, numberOfCalls);
    }

    private LineStringMatch createMatch(final MatchResult matchResult, final LineStringLocation lineStringLocation) {
        final Path path = matchResult.getMergedPath();
        final List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        final List<Integer> matchedLinkIds = pathUtil.determineMatchedLinkIds(flagEncoder, edges);
        final QueryGraph queryGraph = queryGraphExtractor.extractQueryGraph(path);
        final double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        final double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        final double reliability;
        if (ReliabilityCalculationType.POINT_OBSERVATIONS == lineStringLocation.getReliabilityCalculationType()) {
            reliability = calculateCandidatePathScoreOnlyPoints(path, lineStringLocation);
        } else {
            reliability = calculateCandidatePathScore(path, lineStringLocation);
        }
        final LineString lineString = pathUtil.createLineString(path.calcPoints());
        return new LineStringMatch(lineStringLocation, matchedLinkIds, startLinkFraction, endLinkFraction, reliability,
                MatchStatus.MATCH, lineString);
    }

    private double calculateCandidatePathScoreOnlyPoints(final Path path, final LineStringLocation lineStringLocation) {
        final PointList pathPointList = path.calcPoints();
        final CoordinateSequence geometryCoordinates = lineStringLocation.getGeometry().getCoordinateSequence();
        final List<Double> pointDistancesToMatch = new ArrayList<>();
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            final double latitude = geometryCoordinates.getY(index);
            final double longitude = geometryCoordinates.getX(index);
            pointDistancesToMatch.add(calculateSmallestDistanceToPointList(latitude, longitude,
                    pathPointList));
        }
        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - Collections.min(pointDistancesToMatch)
                - Collections.max(pointDistancesToMatch));
    }

    private double calculateCandidatePathScore(final Path path, final LineStringLocation lineStringLocation) {
        final double maximumDistanceInMeters = calculateMaximumDistanceInMeters(path, lineStringLocation.getGeometry());

        final double lengthInMeters = lineStringLocation.getLengthInMeters();
        final double pathDistanceLengthDifferenceInMeters = Math.abs(path.getDistance() - lengthInMeters);

        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - (DISTANCE_PENALTY_FACTOR * maximumDistanceInMeters)
                - (PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthDifferenceInMeters));
    }

    private double calculateMaximumDistanceInMeters(final Path path, final LineString geometry) {
        final PointList pathPointList = path.calcPoints();
        final CoordinateSequence geometryCoordinates = geometry.getCoordinateSequence();
        double maximumDistanceInMeters = 0.0;
        for (int index = 0; index < pathPointList.size(); index++) {
            final double latitude = pathPointList.getLatitude(index);
            final double longitude = pathPointList.getLongitude(index);
            final double smallestDistanceToLtcLink = calculateSmallestDistanceToCoordinateSequence(latitude, longitude,
                    geometryCoordinates);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        for (int index = 0; index < geometryCoordinates.size(); index++) {
            final double latitude = geometryCoordinates.getY(index);
            final double longitude = geometryCoordinates.getX(index);
            final double smallestDistanceToLtcLink = calculateSmallestDistanceToPointList(latitude, longitude,
                    pathPointList);
            maximumDistanceInMeters = Math.max(maximumDistanceInMeters, smallestDistanceToLtcLink);
        }
        return maximumDistanceInMeters;
    }

    private double calculateSmallestDistanceToCoordinateSequence(final double latitude, final double longitude,
            final CoordinateSequence coordinateSequence) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < coordinateSequence.size(); index++) {
            final double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    coordinateSequence.getY(index - 1), coordinateSequence.getX(index - 1),
                    coordinateSequence.getY(index), coordinateSequence.getX(index), REDUCE_TO_SEGMENT);
            final double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }

    private double calculateSmallestDistanceToPointList(final double latitude, final double longitude,
            final PointList pointList) {
        double smallestDistanceToLtcLink = Double.MAX_VALUE;
        for (int index = 1; index < pointList.size(); index++) {
            final double normalizedDistance = distanceCalc.calcNormalizedEdgeDistanceNew(latitude, longitude,
                    pointList.getLatitude(index - 1), pointList.getLongitude(index - 1),
                    pointList.getLatitude(index), pointList.getLongitude(index), REDUCE_TO_SEGMENT);
            final double distanceInMeters = distanceCalc.calcDenormalizedDist(normalizedDistance);
            smallestDistanceToLtcLink = Math.min(smallestDistanceToLtcLink, distanceInMeters);
        }
        return smallestDistanceToLtcLink;
    }

    private LineStringMatch createFailedMatch(final LineStringLocation lineStringLocation, final MatchStatus status) {
        final List<Integer> matchedLinkIds = Lists.newArrayList();
        final double startLinkFraction = 0.0;
        final double endLinkFraction = 0.0;
        final double reliability = 0.0;
        final LineString lineString = lineStringLocation.getGeometry();
        return new LineStringMatch(lineStringLocation,
                matchedLinkIds,
                startLinkFraction,
                endLinkFraction,
                reliability,
                status,
                lineString);
    }
}
