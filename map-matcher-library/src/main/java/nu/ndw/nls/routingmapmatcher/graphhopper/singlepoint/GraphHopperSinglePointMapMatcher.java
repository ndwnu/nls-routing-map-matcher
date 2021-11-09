package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.DistanceCalc;
import com.graphhopper.util.DistanceCalcEarth;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.util.PathUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class GraphHopperSinglePointMapMatcher implements SinglePointMapMatcher {

    /**
     * Only search for candidates within this distance.
     */
    private static final double MAXIMUM_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    /**
     * Distances returned by GraphHopper contain floating point errors compared to the source data, so a delta needs to
     * be used when comparing distances to find all segments that are equally far in the source data.
     */
    private static final double DISTANCE_ROUNDING_ERROR = 0.1;

    private static final int MAX_RELIABILITY_SCORE = 100;

    private final LinkFlagEncoder flagEncoder;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;
    private final GeometryFactory geometryFactory;
    private final PathUtil pathUtil;
    private final QueryGraph queryGraph;
    private final IsochroneService isochroneService;
    private DistanceCalc distanceCalculator;

    public GraphHopperSinglePointMapMatcher(final NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        final List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;

        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(this.geometryFactory);
        this.queryGraph = new QueryGraph(network.getGraphHopperStorage());
        final Weighting weighting = new ShortestWeighting(flagEncoder);
        this.isochroneService = new IsochroneService(flagEncoder, weighting);

        this.distanceCalculator = new DistanceCalcEarth();
    }

    @Override
    public SinglePointMatch match(final SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);

        final List<QueryResult> queryResults = findCandidates(singlePointLocation.getPoint());

        final SinglePointMatch match;
        if (queryResults.isEmpty()) {
            match = createFailedMatch(singlePointLocation);
        } else {
            match = createMatch(queryResults, singlePointLocation);
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

    private SinglePointMatch createMatch(final List<QueryResult> queryResults,
            final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final double closestDistance = queryResults.stream().mapToDouble(QueryResult::getQueryDistance).min()
            .orElse(MAXIMUM_CANDIDATE_DISTANCE_IN_METERS);

        if (singlePointLocation.getUpstreamIsochroneUnit() != null ||
                singlePointLocation.getDownstreamIsochroneUnit() != null) {
            queryGraph.lookup(queryResults);
        }

        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() < closestDistance + DISTANCE_ROUNDING_ERROR) {
                final IntsRef flags = queryResult.getClosestEdge().getFlags();
                final int matchedLinkId = flagEncoder.getId(flags);
                final int nodeId = queryResult.getClosestNode();
                final Set<Integer> upstreamLinkIds = singlePointLocation.getUpstreamIsochroneUnit() != null ?
                        isochroneService.getUpstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;
                final Set<Integer> downstreamLinkIds = singlePointLocation.getDownstreamIsochroneUnit() != null ?
                        isochroneService.getDownstreamLinkIds(queryGraph, singlePointLocation, nodeId) : null;

                final GHPoint3D ghSnappedPoint = queryResult.getSnappedPoint();
                final Point snappedPoint = geometryFactory.createPoint(
                    new Coordinate(ghSnappedPoint.getLon(), ghSnappedPoint.getLat()));

                final double fraction = this.pathUtil.determineSnappedPointFraction(queryResult,
                        this.distanceCalculator);

                candidateMatches.add(new SinglePointMatch.CandidateMatch(matchedLinkId, upstreamLinkIds,
                        downstreamLinkIds, snappedPoint, fraction));
            }
        }

        final double reliability = (1 - closestDistance / MAXIMUM_CANDIDATE_DISTANCE_IN_METERS) * MAX_RELIABILITY_SCORE;
        return new SinglePointMatch(singlePointLocation.getId(), candidateMatches, reliability, MatchStatus.MATCH);
    }

    private SinglePointMatch createFailedMatch(final SinglePointLocation singlePointLocation) {
        final List<SinglePointMatch.CandidateMatch> candidateMatches = Lists.newArrayList();
        final MatchStatus matchStatus = MatchStatus.NO_MATCH;
        final double reliability = 0.0;
        return new SinglePointMatch(singlePointLocation.getId(),candidateMatches, reliability, matchStatus);
    }
}
