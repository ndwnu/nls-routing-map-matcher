package nu.ndw.nls.routingmapmatcher.viterbi;

import static nu.ndw.nls.routingmapmatcher.util.MatchUtil.getQueryResults;

import com.graphhopper.config.Profile;
import com.graphhopper.matching.MapMatching;
import com.graphhopper.matching.MatchResult;
import com.graphhopper.matching.Observation;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraphExtractor;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FiniteWeightFilter;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PathSimplification;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RamerDouglasPeucker;
import com.graphhopper.util.shapes.GHPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcher;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import nu.ndw.nls.routingmapmatcher.util.LineStringMatchUtil;
import nu.ndw.nls.routingmapmatcher.util.LineStringScoreUtil;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

@Slf4j
public class ViterbiLineStringMapMatcher implements
        MapMatcher<LineStringLocation, LineStringMatch> {

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

    /**
     * The tolerance used in smoothing the line before executing map matching
     */
    private static final double LINE_SMOOTHING_TOLERANCE = 0.5D;
    private static final int COORDINATES_LENGTH_START_END = 2;
    private static final String PROFILE_KEY = "profile";

    private final LocationIndexTree locationIndexTree;
    private final NetworkGraphHopper networkGraphHopper;
    private final LineStringMatchUtil lineStringMatchUtil;
    private final LineStringScoreUtil lineStringScoreUtil;
    private final Profile profile;
    private final PointListUtil pointListUtil;
    private final GeometryFactoryWgs84 geometryFactoryWgs84;

    public ViterbiLineStringMapMatcher(NetworkGraphHopper networkGraphHopper, String profileName,
            GeometryFactoryWgs84 geometryFactoryWgs84, FractionAndDistanceCalculator fractionAndDistanceCalculator,
            PointListUtil pointListUtil, double absoluteRelativeWeighingFactor) {
        this.networkGraphHopper = Objects.requireNonNull(networkGraphHopper);
        this.locationIndexTree = networkGraphHopper.getLocationIndex();
        this.geometryFactoryWgs84 = geometryFactoryWgs84;
        this.profile = Objects.requireNonNull(networkGraphHopper.getProfile(profileName));
        this.lineStringMatchUtil = new LineStringMatchUtil(networkGraphHopper, this.profile, fractionAndDistanceCalculator, pointListUtil);
        log.debug("LineStringScoreUtil created with absoluteRelativeWeighingFactor of {}", absoluteRelativeWeighingFactor);
        this.lineStringScoreUtil = new LineStringScoreUtil(fractionAndDistanceCalculator, absoluteRelativeWeighingFactor);
        this.pointListUtil = pointListUtil;
    }

    @Override
    public LineStringMatch match(LineStringLocation lineStringLocation) {
        Objects.requireNonNull(lineStringLocation);
        PointList pointList = PointList.fromLineString(lineStringLocation.getGeometry());
        var simplifier = new RamerDouglasPeucker();
        simplifier.setMaxDistance(LINE_SMOOTHING_TOLERANCE);
        PathSimplification.simplify(pointList, List.of(), simplifier);
        LineString simplifiedLine = pointListUtil.toLineString(pointList);
        LineStringLocation simplifiedLineStringLocation = lineStringLocation.toBuilder().geometry(simplifiedLine)
                .build();

        PMap hints = createHints();
        MapMatching mapMatching = createMapMatching(simplifiedLineStringLocation, hints);
        List<Observation> observations = convertToObservations(simplifiedLineStringLocation.getGeometry());
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

    private MapMatching createMapMatching(LineStringLocation lineStringLocation, PMap hints) {
        MapMatching mapMatching = MapMatching.fromGraphHopper(networkGraphHopper, hints);
        mapMatching.setMeasurementErrorSigma(lineStringLocation.getRadius() == null ? MEASUREMENT_ERROR_SIGMA_IN_METERS
                : lineStringLocation.getRadius());
        mapMatching.setTransitionProbabilityBeta(TRANSITION_PROBABILITY_BETA);
        return mapMatching;
    }

    private PMap createHints() {
        PMap hints = new PMap();
        hints.putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
        hints.putObject(PROFILE_KEY, this.profile.getName());
        hints.putObject(Parameters.CH.DISABLE, true);
        return hints;
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
        Point point = geometryFactoryWgs84.createPoint(
                new Coordinate(observation.getPoint().getLon(), observation.getPoint().getLat()));
        Weighting weighting = networkGraphHopper.createWeighting(profile, createHints());
        EdgeFilter edgeFilter = new FiniteWeightFilter(weighting);
        List<Snap> queryResults = getQueryResults(networkGraphHopper, point,
                MEASUREMENT_ERROR_SIGMA_IN_METERS,
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
        QueryGraph queryGraph = QueryGraphExtractor.extractQueryGraph(path);
        double reliability = lineStringScoreUtil.calculateCandidatePathScore(path, lineStringLocation);
        return lineStringMatchUtil.createMatch(lineStringLocation, path, queryGraph, reliability);
    }
}
