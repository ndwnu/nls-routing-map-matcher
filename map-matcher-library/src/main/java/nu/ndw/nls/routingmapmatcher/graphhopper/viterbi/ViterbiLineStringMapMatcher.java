package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.CAR_SHORTEST;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;

import com.google.common.base.Preconditions;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringMatchUtil;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringScoreUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

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
     * See also the comment in {@link #convertToObservations(LineString)}
     */
    private static final double NEARBY_NDW_NETWORK_DISTANCE_IN_METERS = 2 * MEASUREMENT_ERROR_SIGMA_IN_METERS;

    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);


    private static final int COORDINATES_LENGTH_START_END = 2;
    public static final String PROFILE_KEY = "profile";

    private final MapMatching mapMatching;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final NetworkGraphHopper networkGraphHopper;
    private final QueryGraphExtractor queryGraphExtractor;
    private final LineStringMatchUtil lineStringMatchUtil;
    private final LineStringScoreUtil lineStringScoreUtil;

    public ViterbiLineStringMapMatcher(NetworkGraphHopper networkGraphHopper) {
        Preconditions.checkNotNull(networkGraphHopper);
        this.networkGraphHopper = networkGraphHopper;
        PMap hints = new PMap();
        hints.putObject(PROFILE_KEY, CAR_SHORTEST);
        hints.putObject(Parameters.CH.DISABLE, true);
        this.mapMatching = MapMatching.fromGraphHopper(networkGraphHopper, hints);
        mapMatching.setMeasurementErrorSigma(MEASUREMENT_ERROR_SIGMA_IN_METERS);
        mapMatching.setTransitionProbabilityBeta(TRANSITION_PROBABILITY_BETA);
        this.locationIndexTree = networkGraphHopper.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        this.queryGraphExtractor = new QueryGraphExtractor();
        this.lineStringMatchUtil = new LineStringMatchUtil(networkGraphHopper);
        this.lineStringScoreUtil = new LineStringScoreUtil();
    }

    @Override
    public LineStringMatch match(LineStringLocation lineStringLocation) {
        Preconditions.checkNotNull(lineStringLocation);
        List<Observation> observations = convertToObservations(lineStringLocation.getGeometry());
        LineStringMatch lineStringMatch;
        if (observations.size() >= COORDINATES_LENGTH_START_END) {
            try {

                MatchResult matchResult = mapMatching.match(observations);

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

    private List<Observation> convertToObservations(LineString lineString) {
        CoordinateSequence coordinateSequence = lineString.getCoordinateSequence();
        List<Observation> observations = new ArrayList<>();
        for (int index = 0; index < coordinateSequence.size(); index++) {
            Observation observation = new Observation(
                    new GHPoint(coordinateSequence.getY(index), coordinateSequence.getX(index)));
            // Only add observation entry when coordinate is nearby the NDW base network.
            // This way, when an empty observation list is
            // returned, we can be pretty confident that there is no matching possible on the NDW base network.
            if (isNearbyNdwNetwork(observation)) {
                observations.add(observation);
            }

        }
        return observations;
    }


    private boolean isNearbyNdwNetwork(Observation observation) {
        Point point = WGS84_GEOMETRY_FACTORY.createPoint(
                new Coordinate(observation.getPoint().getLon(), observation.getPoint().getLat()));
        List<Snap> queryResults = getQueryResults(networkGraphHopper, point, MEASUREMENT_ERROR_SIGMA_IN_METERS,
                locationIndexTree, edgeFilter);
        for (Snap queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= NEARBY_NDW_NETWORK_DISTANCE_IN_METERS) {
                return true;
            }
        }
        return false;
    }

    private LineStringMatch createMatch(MatchResult matchResult, LineStringLocation lineStringLocation) {
        Path path = matchResult.getMergedPath();
        QueryGraph queryGraph = queryGraphExtractor.extractQueryGraph(path);
        double reliability = lineStringScoreUtil.calculateCandidatePathScore(path, lineStringLocation);
        return lineStringMatchUtil.createMatch(lineStringLocation, path, queryGraph, reliability);
    }
}
