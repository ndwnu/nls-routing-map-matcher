package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
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
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConstants;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.PathUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphHopperStartToEndMapMatcher implements StartToEndMapMatcher {

    /**
     * Only search for candidates within this distance.
     */
    private static final double MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    /**
     * Parameter for sanity check of the input data.
     * <p>
     * When the distance as the crow flies between start and end coordinate is larger than "length
     * affected" * {@value #SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR} and
     * larger than "length affected" +
     * {@value #SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_EXTRA_METERS}m then the input data in
     * considered invalid.
     */
    private static final int SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR = 2;

    /**
     * Parameter for sanity check of the input data.
     * <p>
     *
     * @see {@link #SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR}
     */
    private static final int SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_EXTRA_METERS = 50;
    private static final int MIN_RELIABILITY_SCORE = 0;
    private static final int MAX_RELIABILITY_SCORE = 100;
    private static final double PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR = 0.1;

    private final LinkFlagEncoder flagEncoder;
    private final Graph routingGraph;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;

    private final DistanceCalc distanceCalc;

    private final RoutingAlgorithmFactory algorithmFactory;
    private final AlgorithmOptions algorithmOptions;

    private final GeometryFactory geometryFactory;
    private final PathUtil pathUtil;

    public GraphHopperStartToEndMapMatcher(final NetworkGraphHopper graphHopper) {
        Preconditions.checkNotNull(graphHopper);
        final List<FlagEncoder> flagEncoders = graphHopper.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.routingGraph = graphHopper.getGraphHopperStorage();
        this.locationIndexTree = (LocationIndexTree) graphHopper.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;

        this.distanceCalc = GraphHopperConstants.distanceCalculation;

        final HintsMap hints = new HintsMap();
        hints.put(Parameters.CH.DISABLE, true);
        hints.setVehicle(flagEncoder.toString());
        this.algorithmFactory = graphHopper.getAlgorithmFactory(hints);

        final String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.algorithmOptions = new AlgorithmOptions(algorithm, weighting);

        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(geometryFactory);
    }

    /**
     * TODO
     * - Make lengthAffected optional: driving distance between start and end points is not always known to caller
     * - Remove sanity check: lengthAffected is only used to calculate reliability, no reason to reject entire request
     * - Align calculation of reliability with ViterbiLineStringMapMatcher:
     *   - If lengthAffected is provided, calculate reliability based on point distance and length difference
     *     (like calculateCandidatePathScore / ReliabilityCalculationType.LINE_STRING)
     *   - If lengthAffected is not provided, calculate reliability based on point distance only
     *     (like calculateCandidatePathScoreOnlyPoints / ReliabilityCalculationType.POINT_OBSERVATIONS)
     */
    public StartToEndMatch match(final StartToEndLocation startToEndLocation) {
        Preconditions.checkNotNull(startToEndLocation);

        final Point startPoint = startToEndLocation.getStartPoint();
        final Point endPoint = startToEndLocation.getEndPoint();
        final double distanceAsTheCrowFliesInMeters = distanceCalc.calcDist(startPoint.getY(), startPoint.getX(),
                endPoint.getY(), endPoint.getX());

        final StartToEndMatch match;
        if (!sanityCheckPassed(startToEndLocation, distanceAsTheCrowFliesInMeters)) {
            match = createFailedMatch(startToEndLocation, MatchStatus.INVALID_INPUT);
        } else {
            match = findMatch(startToEndLocation);
        }

        return match;
    }

    private boolean sanityCheckPassed(final StartToEndLocation startToEndLocation,
            final double distanceAsTheCrowFliesInMeters) {
        final double lengthAffected = startToEndLocation.getLengthAffected();
        double maximumDistanceAsTheCrowFliesInMeters =
                SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR * lengthAffected;
        maximumDistanceAsTheCrowFliesInMeters = Math.max(maximumDistanceAsTheCrowFliesInMeters, lengthAffected +
                SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_EXTRA_METERS);
        return distanceAsTheCrowFliesInMeters <= maximumDistanceAsTheCrowFliesInMeters;
    }

    private StartToEndMatch createFailedMatch(final StartToEndLocation startToEndLocation, final MatchStatus status) {
        final int id = startToEndLocation.getId();
        final int locationIndex = startToEndLocation.getLocationIndex();
        final List<Integer> ndwLinkIds = Lists.newArrayList();
        final double startLinkFraction = 0.0;
        final double endLinkFraction = 0.0;
        final double reliability = 0.0;
        final LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                startToEndLocation.getStartPoint().getCoordinate(),
                startToEndLocation.getEndPoint().getCoordinate()});
        return new StartToEndMatch(id, locationIndex, ndwLinkIds, startLinkFraction, endLinkFraction, reliability,
                status, lineString);
    }

    private StartToEndMatch findMatch(final StartToEndLocation startToEndLocation) {
        List<QueryResult> startCandidates = findCandidates(startToEndLocation.getStartPoint());
        List<QueryResult> endCandidates = findCandidates(startToEndLocation.getEndPoint());

        final QueryGraph queryGraph = createQueryGraphAndAssignClosestNodePerCandidate(startCandidates, endCandidates);
        startCandidates = deduplicateCandidatesByClosestNode(startCandidates);
        endCandidates = deduplicateCandidatesByClosestNode(endCandidates);

        final List<Path> candidatePaths = createCandidatePaths(queryGraph, startCandidates, endCandidates);

        final StartToEndMatch match;
        if (!candidatePaths.isEmpty()) {
            final Path path = chooseBestCandidatePath(candidatePaths, startToEndLocation);
            match = createMatch(startToEndLocation, path, queryGraph);
        } else {
            match = createFailedMatch(startToEndLocation, MatchStatus.NO_MATCH);
        }

        return match;
    }

    private List<QueryResult> findCandidates(final Point point) {
        final double latitude = point.getY();
        final double longitude = point.getX();
        final List<QueryResult> queryResults = locationIndexTree.findNClosest(latitude, longitude, edgeFilter,
                MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);
        final List<QueryResult> candidates = new ArrayList<>(queryResults.size());
        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= MAXIMUM_CANDIDATE_DISTANCE_IN_METERS) {
                candidates.add(queryResult);
            }
        }
        return candidates;
    }

    private QueryGraph createQueryGraphAndAssignClosestNodePerCandidate(final List<QueryResult> startCandidates,
            final List<QueryResult> endCandidates) {
        final List<QueryResult> allCandidates = new ArrayList<>(startCandidates.size() + endCandidates.size());
        allCandidates.addAll(startCandidates);
        allCandidates.addAll(endCandidates);
        final QueryGraph queryGraph = new QueryGraph(routingGraph);
        queryGraph.setUseEdgeExplorerCache(true);
        queryGraph.lookup(allCandidates);
        return queryGraph;
    }

    private List<QueryResult> deduplicateCandidatesByClosestNode(final List<QueryResult> candidates) {
        final List<QueryResult> deduplicatedCandidates = new ArrayList<>(candidates.size());
        final Map<Integer, QueryResult> candidatePerClosestNode = new HashMap<>();
        for (final QueryResult queryResult : candidates) {
            candidatePerClosestNode.put(queryResult.getClosestNode(), queryResult);
        }
        deduplicatedCandidates.addAll(candidatePerClosestNode.values());
        return deduplicatedCandidates;
    }

    private List<Path> createCandidatePaths(final QueryGraph queryGraph, final List<QueryResult> startCandidates,
            final List<QueryResult> endCandidates) {
        final List<Path> candidatePaths = new ArrayList<>(startCandidates.size() * endCandidates.size());
        for (final QueryResult startCandidate : startCandidates) {
            for (final QueryResult endCandidate : endCandidates) {
                final int fromNode = startCandidate.getClosestNode();
                final int toNode = endCandidate.getClosestNode();

                final RoutingAlgorithm routingAlgorithm = algorithmFactory.createAlgo(queryGraph, algorithmOptions);
                final Path path = routingAlgorithm.calcPath(fromNode, toNode);

                if (path.isFound() && path.getEdgeCount() > 0) {
                    candidatePaths.add(path);
                }
            }
        }
        return candidatePaths;
    }

    private Path chooseBestCandidatePath(final List<Path> candidatePaths, final StartToEndLocation startToEndLocation) {
        Path bestCandidatePath = candidatePaths.get(0);
        double bestCandidatePathScore = calculateCandidatePathScore(candidatePaths.get(0), startToEndLocation);
        for (int index = 1; index < candidatePaths.size(); index++) {
            final Path candidatePath = candidatePaths.get(index);
            final double candidateScore = calculateCandidatePathScore(candidatePath, startToEndLocation);
            if (candidateScore > bestCandidatePathScore) {
                bestCandidatePath = candidatePath;
                bestCandidatePathScore = candidateScore;
            }
        }
        return bestCandidatePath;
    }

    private double calculateCandidatePathScore(final Path path, final StartToEndLocation startToEndLocation) {
        final PointList points = path.calcPoints();
        if (points.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no points");
        }
        final Point startPoint = startToEndLocation.getStartPoint();
        final double startPointDistanceInMeters = distanceCalc.calcDist(startPoint.getY(), startPoint.getX(),
                points.getLatitude(0), points.getLongitude(0));

        final Point endPoint = startToEndLocation.getEndPoint();
        final double endPointDistanceInMeters = distanceCalc.calcDist(endPoint.getY(), endPoint.getX(),
                points.getLatitude(points.size() - 1), points.getLongitude(points.size() - 1));

        final double lengthAffectedInMeters = startToEndLocation.getLengthAffected();
        final double pathDistanceLengthAffectedDifferenceInMeters =
                Math.abs(path.getDistance() - lengthAffectedInMeters);

        return Math.max(MIN_RELIABILITY_SCORE, MAX_RELIABILITY_SCORE
                - startPointDistanceInMeters - endPointDistanceInMeters
                - (PATH_LENGTH_DIFFERENCE_PENALTY_FACTOR * pathDistanceLengthAffectedDifferenceInMeters));
    }

    private StartToEndMatch createMatch(final StartToEndLocation startToEndLocation, final Path path,
            final QueryGraph queryGraph) {
        final List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        final List<Integer> ndwLinkIds = pathUtil.determineMatchedLinkIds(flagEncoder, edges);
        final double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        final double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        final double reliability = calculateCandidatePathScore(path, startToEndLocation);
        final LineString lineString = pathUtil.createLineString(path.calcPoints());
        final int id = startToEndLocation.getId();
        final int locationIndex = startToEndLocation.getLocationIndex();
        return new StartToEndMatch(id, locationIndex, ndwLinkIds, startLinkFraction, endLinkFraction, reliability,
                MatchStatus.MATCH, lineString);
    }
}
