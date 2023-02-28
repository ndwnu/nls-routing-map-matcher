package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;

import com.google.common.base.Preconditions;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.RoutingAlgorithmFactory;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.HintsMap;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.Parameters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringMatchUtil;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.LineStringScoreUtil;
import org.locationtech.jts.geom.Point;

public class GraphHopperStartToEndMapMatcher implements StartToEndMapMatcher {

    /**
     * Only search for candidates within this distance.
     */
    private static final double MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    private final Graph routingGraph;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final RoutingAlgorithmFactory algorithmFactory;
    private final AlgorithmOptions algorithmOptions;

    private final LineStringMatchUtil lineStringMatchUtil;
    private final LineStringScoreUtil lineStringScoreUtil;

    public GraphHopperStartToEndMapMatcher(NetworkGraphHopper graphHopper) {
        Preconditions.checkNotNull(graphHopper);
        List<FlagEncoder> flagEncoders = graphHopper.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        LinkFlagEncoder flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.routingGraph = graphHopper.getGraphHopperStorage();
        this.locationIndexTree = (LocationIndexTree) graphHopper.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;

        HintsMap hints = new HintsMap();
        hints.put(Parameters.CH.DISABLE, true);
        hints.setVehicle(flagEncoder.toString());
        this.algorithmFactory = graphHopper.getAlgorithmFactory(hints);

        String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
        Weighting weighting = new ShortestWeighting(flagEncoder);
        this.algorithmOptions = new AlgorithmOptions(algorithm, weighting);

        this.lineStringMatchUtil = new LineStringMatchUtil(flagEncoder, weighting);
        this.lineStringScoreUtil = new LineStringScoreUtil();
    }

    public LineStringMatch match(LineStringLocation lineStringLocation) {
        Preconditions.checkNotNull(lineStringLocation);

        List<QueryResult> startCandidates = findCandidates(lineStringLocation.getGeometry().getStartPoint());
        List<QueryResult> endCandidates = findCandidates(lineStringLocation.getGeometry().getEndPoint());

        QueryGraph queryGraph = createQueryGraphAndAssignClosestNodePerCandidate(startCandidates, endCandidates);
        startCandidates = deduplicateCandidatesByClosestNode(startCandidates);
        endCandidates = deduplicateCandidatesByClosestNode(endCandidates);

        List<Candidate> candidatePaths = createCandidatePaths(queryGraph, startCandidates, endCandidates,
                lineStringLocation);

        return candidatePaths.stream()
                .max(Comparator.comparingDouble(Candidate::getScore))
                .map(candidate -> lineStringMatchUtil.createMatch(lineStringLocation, candidate.getPath(), queryGraph,
                        candidate.getScore()))
                .orElseGet(() -> lineStringMatchUtil.createFailedMatch(lineStringLocation, MatchStatus.NO_MATCH));
    }

    private List<QueryResult> findCandidates(Point point) {
        return getQueryResults(point, MAXIMUM_CANDIDATE_DISTANCE_IN_METERS, locationIndexTree, edgeFilter);
    }

    private QueryGraph createQueryGraphAndAssignClosestNodePerCandidate(List<QueryResult> startCandidates,
            List<QueryResult> endCandidates) {
        List<QueryResult> allCandidates = new ArrayList<>(startCandidates.size() + endCandidates.size());
        allCandidates.addAll(startCandidates);
        allCandidates.addAll(endCandidates);
        QueryGraph queryGraph = new QueryGraph(routingGraph);
        queryGraph.setUseEdgeExplorerCache(true);
        queryGraph.lookup(allCandidates);
        return queryGraph;
    }

    private List<QueryResult> deduplicateCandidatesByClosestNode(List<QueryResult> candidates) {
        List<QueryResult> deduplicatedCandidates = new ArrayList<>(candidates.size());
        Map<Integer, QueryResult> candidatePerClosestNode = new HashMap<>();
        for (QueryResult queryResult : candidates) {
            candidatePerClosestNode.put(queryResult.getClosestNode(), queryResult);
        }
        deduplicatedCandidates.addAll(candidatePerClosestNode.values());
        return deduplicatedCandidates;
    }

    private List<Candidate> createCandidatePaths(QueryGraph queryGraph, List<QueryResult> startCandidates,
            List<QueryResult> endCandidates, LineStringLocation lineStringLocation) {
        List<Candidate> candidatePaths = new ArrayList<>(startCandidates.size() * endCandidates.size());
        for (QueryResult startCandidate : startCandidates) {
            for (QueryResult endCandidate : endCandidates) {
                int fromNode = startCandidate.getClosestNode();
                int toNode = endCandidate.getClosestNode();

                RoutingAlgorithm routingAlgorithm = algorithmFactory.createAlgo(queryGraph, algorithmOptions);
                Path path = routingAlgorithm.calcPath(fromNode, toNode);

                if (path.isFound() && path.getEdgeCount() > 0) {
                    double score = lineStringScoreUtil.calculateCandidatePathScore(path, lineStringLocation);
                    candidatePaths.add(new Candidate(path, score));
                }
            }
        }
        return candidatePaths;
    }

    @Value
    private static class Candidate {

        Path path;
        double score;
    }
}
