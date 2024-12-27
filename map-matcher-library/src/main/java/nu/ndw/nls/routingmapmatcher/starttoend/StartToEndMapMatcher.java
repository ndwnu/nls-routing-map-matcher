package nu.ndw.nls.routingmapmatcher.starttoend;

import static com.graphhopper.util.Parameters.Algorithms.DIJKSTRA_BI;
import static nu.ndw.nls.routingmapmatcher.util.MatchUtil.getQueryResults;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.RoutingAlgorithmFactorySimple;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FiniteWeightFilter;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.CustomModel;
import com.graphhopper.util.PMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcher;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.Constants;
import nu.ndw.nls.routingmapmatcher.util.LineStringMatchUtil;
import nu.ndw.nls.routingmapmatcher.util.LineStringScoreUtil;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.locationtech.jts.geom.Point;

public class StartToEndMapMatcher implements MapMatcher<LineStringLocation, LineStringMatch> {

    /**
     * Only search for candidates within this distance.
     */
    private static final double MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    private final BaseGraph routingGraph;
    private final NetworkGraphHopper networkGraphHopper;
    private final LocationIndexTree locationIndexTree;
    private final RoutingAlgorithmFactory algorithmFactory;
    private final AlgorithmOptions algorithmOptions;
    private final LineStringMatchUtil lineStringMatchUtil;
    private final LineStringScoreUtil lineStringScoreUtil;
    private final Weighting weighting;

    public StartToEndMapMatcher(NetworkGraphHopper networkGraphHopper, String profileName,
            FractionAndDistanceCalculator fractionAndDistanceCalculator, PointListUtil pointListUtil) {
        this.networkGraphHopper = Objects.requireNonNull(networkGraphHopper);
        Profile profile = Objects.requireNonNull(networkGraphHopper.getProfile(profileName));
        this.routingGraph = networkGraphHopper.getBaseGraph();
        this.algorithmOptions = new AlgorithmOptions()
                .setAlgorithm(DIJKSTRA_BI)
                .setTraversalMode(TraversalMode.NODE_BASED);
        this.algorithmFactory = new RoutingAlgorithmFactorySimple();
        this.locationIndexTree = networkGraphHopper.getLocationIndex();
        this.lineStringMatchUtil = new LineStringMatchUtil(networkGraphHopper, profile, fractionAndDistanceCalculator,
                pointListUtil);
        this.lineStringScoreUtil = new LineStringScoreUtil(fractionAndDistanceCalculator);
        this.weighting = networkGraphHopper.createWeighting(profile, createShortestDistanceHints());
    }

    private PMap createShortestDistanceHints() {
        return new PMap()
                .putObject(CustomModel.KEY, Constants.SHORTEST_CUSTOM_MODEL);
    }

    public LineStringMatch match(LineStringLocation lineStringLocation) {
        Objects.requireNonNull(lineStringLocation);

        List<Snap> startCandidates = findCandidates(lineStringLocation.getGeometry().getStartPoint());
        List<Snap> endCandidates = findCandidates(lineStringLocation.getGeometry().getEndPoint());

        QueryGraph queryGraph = createQueryGraphAndAssignClosestNodePerCandidate(startCandidates, endCandidates);
        startCandidates = deduplicateCandidatesByClosestNode(startCandidates);
        endCandidates = deduplicateCandidatesByClosestNode(endCandidates);

        List<Candidate> candidatePaths = createCandidatePaths(queryGraph, startCandidates, endCandidates,
                lineStringLocation);

        return candidatePaths.stream()
                .max(Comparator.comparingDouble(Candidate::score))
                .map(candidate -> lineStringMatchUtil.createMatch(lineStringLocation, candidate.path(), queryGraph,
                        candidate.score()))
                .orElseGet(() -> lineStringMatchUtil.createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH));
    }

    private List<Snap> findCandidates(Point point) {
        EdgeFilter edgeFilter = new FiniteWeightFilter(weighting);
        return getQueryResults(networkGraphHopper, point,
                MAXIMUM_CANDIDATE_DISTANCE_IN_METERS,
                locationIndexTree, edgeFilter);
    }

    private QueryGraph createQueryGraphAndAssignClosestNodePerCandidate(List<Snap> startCandidates,
            List<Snap> endCandidates) {
        List<Snap> allCandidates = new ArrayList<>(startCandidates.size() + endCandidates.size());
        allCandidates.addAll(startCandidates);
        allCandidates.addAll(endCandidates);
        return QueryGraph.create(routingGraph, allCandidates);
    }

    private List<Snap> deduplicateCandidatesByClosestNode(List<Snap> candidates) {
        List<Snap> deduplicatedCandidates = new ArrayList<>(candidates.size());
        Map<Integer, Snap> candidatePerClosestNode = new HashMap<>();
        for (Snap queryResult : candidates) {
            candidatePerClosestNode.put(queryResult.getClosestNode(), queryResult);
        }
        deduplicatedCandidates.addAll(candidatePerClosestNode.values());
        return deduplicatedCandidates;
    }

    private List<Candidate> createCandidatePaths(QueryGraph queryGraph, List<Snap> startCandidates,
            List<Snap> endCandidates, LineStringLocation lineStringLocation) {
        List<Candidate> candidatePaths = new ArrayList<>(startCandidates.size() * endCandidates.size());
        for (Snap startCandidate : startCandidates) {
            for (Snap endCandidate : endCandidates) {
                int fromNode = startCandidate.getClosestNode();
                int toNode = endCandidate.getClosestNode();
                RoutingAlgorithm routingAlgorithm = algorithmFactory.createAlgo(queryGraph, weighting,
                        algorithmOptions);
                Path path = routingAlgorithm.calcPath(fromNode, toNode);
                if (path.isFound() && path.getEdgeCount() > 0) {
                    double score = lineStringScoreUtil.calculateCandidatePathScore(path, lineStringLocation);
                    candidatePaths.add(new Candidate(path, score));
                }
            }
        }
        return candidatePaths;
    }

    private record Candidate(Path path, double score) {

    }
}
