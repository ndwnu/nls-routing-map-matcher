package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.google.common.base.Preconditions;
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
import com.graphhopper.util.GPXEntry;
import com.graphhopper.util.Parameters;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.CustomDistanceCalc;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringMatchUtil;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringScoreUtil;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

@Slf4j
public class ViterbiLineStringMapMatcher implements LineStringMapMatcher {

    /**
     * The standard deviation of GPS observations.
     * <p>
     * Only search for candidates within this distance.
     */
    private static final double MEASUREMENT_ERROR_SIGMA_IN_METERS = 20.0;

    /**
     * The beta (1/lambda) parameter used for the exponential distribution to determine the probability that the length
     * of a route between two successive GPS observations is the same as the distance as the crow flies between those
     * GPS observations.
     */
    private static final double TRANSITION_PROBABILITY_BETA = 100.0;

    /**
     * When creating a GPS track, only create GPS "observations" for coordinates that are within this distance of the
     * NDW base network.
     * <p>
     * See also the comment in {@link #createGpsTrack(LineString)}
     */
    private static final double NEARBY_NDW_NETWORK_DISTANCE_IN_METERS = 2 * MEASUREMENT_ERROR_SIGMA_IN_METERS;
    private static final double DISTANCE_CALCULATOR_CUSTOM_DISTANCE = 3 * MEASUREMENT_ERROR_SIGMA_IN_METERS;

    /**
     * Speed to use when creating a GPS track from a geometry.
     * <p>
     * This value does not affect the matching.
     */
    private static final double GPS_TRACK_SPEED_IN_METERS_PER_SECOND = 3.0;

    private static final int MILLIS_PER_SECOND = 1000;

    private static final int COORDINATES_LENGTH_START_END = 2;

    private final MapMatching mapMatching;
    private final CustomDistanceCalc distanceCalc;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;
    private final QueryGraphExtractor queryGraphExtractor;
    private final LineStringMatchUtil lineStringMatchUtil;
    private final LineStringScoreUtil lineStringScoreUtil;

    public ViterbiLineStringMapMatcher(NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);
        LinkFlagEncoder flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
        Weighting weighting = new ShortestWeighting(flagEncoder);
        this.mapMatching = new MapMatching(network, new AlgorithmOptions(algorithm, weighting));
        mapMatching.setMeasurementErrorSigma(MEASUREMENT_ERROR_SIGMA_IN_METERS);
        mapMatching.setTransitionProbabilityBeta(TRANSITION_PROBABILITY_BETA);
        this.distanceCalc = new CustomDistanceCalc();
        mapMatching.setDistanceCalc(distanceCalc);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        this.queryGraphExtractor = new QueryGraphExtractor();
        this.lineStringMatchUtil = new LineStringMatchUtil(flagEncoder, weighting);
        this.lineStringScoreUtil = new LineStringScoreUtil();
    }

    @Override
    public LineStringMatch match(LineStringLocation lineStringLocation) {
        Preconditions.checkNotNull(lineStringLocation);

        List<GPXEntry> gpsTrack = createGpsTrack(lineStringLocation.getGeometry());

        LineStringMatch lineStringMatch;
        if (gpsTrack.size() >= COORDINATES_LENGTH_START_END) {
            try {
                preventFilteringWhileMapMatching(gpsTrack);
                MatchResult matchResult = mapMatching.doWork(gpsTrack);
                if (matchResult.getMergedPath().getEdgeCount() > 0) {
                    lineStringMatch = createMatch(matchResult, lineStringLocation);
                } else {
                    lineStringMatch = lineStringMatchUtil.createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH);
                }
            } catch (RuntimeException e) {
                log.debug("Exception while map matching, creating failed result for {}", lineStringLocation, e);
                lineStringMatch = lineStringMatchUtil.createFailedMatch(lineStringLocation, MatchStatus.EXCEPTION);
            }
        } else {
            lineStringMatch = lineStringMatchUtil.createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH);
        }
        return lineStringMatch;
    }

    private List<GPXEntry> createGpsTrack(LineString lineString) {
        CoordinateSequence coordinateSequence = lineString.getCoordinateSequence();

        List<GPXEntry> gpsTrack = new ArrayList<>();
        long previousTimestampInMillis = 0;
        for (int index = 0; index < coordinateSequence.size(); index++) {
            long timestampInMillis;
            if (index == 0) {
                timestampInMillis = previousTimestampInMillis;
            } else {
                double distanceInMeters = distanceCalc.calcDist(
                        coordinateSequence.getY(index - 1), coordinateSequence.getX(index - 1),
                        coordinateSequence.getY(index), coordinateSequence.getX(index));
                double durationInSeconds = distanceInMeters / GPS_TRACK_SPEED_IN_METERS_PER_SECOND;
                timestampInMillis = previousTimestampInMillis + Math.round(durationInSeconds * MILLIS_PER_SECOND);
            }
            GPXEntry gpxEntry = new GPXEntry(coordinateSequence.getY(index), coordinateSequence.getX(index),
                    timestampInMillis);
            // Only add gpx entry when coordinate is nearby the NDW base network. This way, when an empty GPS track is
            // returned, we can be pretty confident that there is no matching possible on the NDW base network.
            if (isNearbyNdwNetwork(gpxEntry)) {
                gpsTrack.add(gpxEntry);
            }
            previousTimestampInMillis = timestampInMillis;
        }
        return gpsTrack;
    }

    private boolean isNearbyNdwNetwork(GPXEntry gpxEntry) {
        List<QueryResult> queryResults = locationIndexTree.findNClosest(gpxEntry.getLat(), gpxEntry.getLon(),
                edgeFilter, MEASUREMENT_ERROR_SIGMA_IN_METERS);
        for (QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= NEARBY_NDW_NETWORK_DISTANCE_IN_METERS) {
                return true;
            }
        }
        return false;
    }

    private void preventFilteringWhileMapMatching(List<GPXEntry> gpsTrack) {
        // When filtering there is no distance calculation for the first and last GPS coordinates
        int numberOfCalls = Math.max(gpsTrack.size() - COORDINATES_LENGTH_START_END, 0);
        distanceCalc.returnCustomDistanceForNextCalls(DISTANCE_CALCULATOR_CUSTOM_DISTANCE, numberOfCalls);
    }

    private LineStringMatch createMatch(MatchResult matchResult, LineStringLocation lineStringLocation) {
        Path path = matchResult.getMergedPath();
        QueryGraph queryGraph = queryGraphExtractor.extractQueryGraph(path);
        double reliability = lineStringScoreUtil.calculateCandidatePathScore(path, lineStringLocation);
        return lineStringMatchUtil.createMatch(lineStringLocation, path, queryGraph, reliability);
    }
}
