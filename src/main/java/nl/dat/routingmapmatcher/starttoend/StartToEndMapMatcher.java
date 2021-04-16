package nl.dat.routingmapmatcher.starttoend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

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

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.enums.MatchStatus;
import nl.dat.routingmapmatcher.exceptions.RoutingMapMatcherException;
import nl.dat.routingmapmatcher.graphhopper.GraphHopperConstants;
import nl.dat.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nl.dat.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nl.dat.routingmapmatcher.util.PathUtil;

public class StartToEndMapMatcher {

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
   * @see {@link #SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR}
   */
  private static final int SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_EXTRA_METERS = 50;

  private final LinkFlagEncoder flagEncoder;
  private final Graph routingGraph;
  private final LocationIndexTree locationIndexTree;
  private final EdgeFilter edgeFilter;

  private final DistanceCalc distanceCalc;

  private final RoutingAlgorithmFactory algorithmFactory;
  private final AlgorithmOptions algorithmOptions;

  private final GeometryFactory geometryFactory;
  private final PathUtil pathUtil;

  public StartToEndMapMatcher(final NetworkGraphHopper ndwNetwork) {
    Preconditions.checkNotNull(ndwNetwork);
    final List<FlagEncoder> flagEncoders = ndwNetwork.getEncodingManager().fetchEdgeEncoders();
    Preconditions.checkArgument(flagEncoders.size() == 1);
    Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

    this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
    this.routingGraph = ndwNetwork.getGraphHopperStorage();
    this.locationIndexTree = (LocationIndexTree) ndwNetwork.getLocationIndex();
    this.edgeFilter = EdgeFilter.ALL_EDGES;

    this.distanceCalc = GraphHopperConstants.distanceCalculation;

    final HintsMap hints = new HintsMap();
    hints.put(Parameters.CH.DISABLE, true);
    hints.setVehicle(flagEncoder.toString());
    this.algorithmFactory = ndwNetwork.getAlgorithmFactory(hints);

    final String algorithm = Parameters.Algorithms.DIJKSTRA_BI;
    final Weighting weighting = new ShortestWeighting(flagEncoder);
    this.algorithmOptions = new AlgorithmOptions(algorithm, weighting);

    this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    this.pathUtil = new PathUtil(geometryFactory);
  }

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
    double maximumDistanceAsTheCrosFliesInMeters =
        SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_LENGTH_AFFECTED_FACTOR * lengthAffected;
    maximumDistanceAsTheCrosFliesInMeters = Math.max(maximumDistanceAsTheCrosFliesInMeters, lengthAffected +
        SANITY_CHECK_MAX_DISTANCE_AS_THE_CROW_FLIES_EXTRA_METERS);
    return distanceAsTheCrowFliesInMeters <= maximumDistanceAsTheCrosFliesInMeters;
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
      match = createFailedMatch(startToEndLocation, MatchStatus.NO_PATH);
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

  private Path chooseBestCandidatePath(final List<Path> candidatePaths,
      final StartToEndLocation startToEndLocation) {
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
    final double pathDistanceLengthAffectedDifferenceInMeters = Math.abs(path.getDistance() - lengthAffectedInMeters);

    return Math.max(0, 100 - startPointDistanceInMeters - endPointDistanceInMeters -
        (0.1 * pathDistanceLengthAffectedDifferenceInMeters));
  }

  private StartToEndMatch createMatch(final StartToEndLocation startToEndLocation, final Path path,
      final QueryGraph queryGraph) {
    final int id = startToEndLocation.getId();
    final int locationIndex = startToEndLocation.getLocationIndex();

    final List<EdgeIteratorState> edges = path.calcEdges();
    if (edges.isEmpty()) {
      throw new RoutingMapMatcherException("Unexpected: path has no edges");
    }
    final List<Integer> ndwLinkIds = pathUtil.determineNdwLinkIds(flagEncoder, edges);
    final double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
    final double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
    final double reliability = calculateCandidatePathScore(path, startToEndLocation);
    final LineString lineString = pathUtil.createLineString(path.calcPoints());
    return new StartToEndMatch(id, locationIndex, ndwLinkIds, startLinkFraction, endLinkFraction, reliability,
        MatchStatus.MATCH, lineString);
  }
}
