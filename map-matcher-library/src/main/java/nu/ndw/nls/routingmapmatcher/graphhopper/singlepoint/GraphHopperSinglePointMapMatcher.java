package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.shapes.GHPoint3D;
import java.util.ArrayList;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
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
     * To specify 1 decimal places of precision, use a scale factor of 10 (i.e. rounding to the nearest 10).
     */
    private static final double DISTANCE_SCALE_FACTOR = 10.0;

    private final LinkFlagEncoder flagEncoder;
    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;
    private final GeometryFactory geometryFactory;

    public GraphHopperSinglePointMapMatcher(final NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        final List<FlagEncoder> flagEncoders = network.getEncodingManager().fetchEdgeEncoders();
        Preconditions.checkArgument(flagEncoders.size() == 1);
        Preconditions.checkArgument(flagEncoders.get(0) instanceof LinkFlagEncoder);

        this.flagEncoder = (LinkFlagEncoder) flagEncoders.get(0);
        this.locationIndexTree = (LocationIndexTree) network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;

        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    public SinglePointMatch match(final Point point) {
        Preconditions.checkNotNull(point);

        final List<QueryResult> queryResults = findCandidates(point);

        final SinglePointMatch match;
        if (queryResults.isEmpty()) {
            match = createFailedMatch();
        } else {
            match = createMatch(queryResults);
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

    private SinglePointMatch createMatch(List<QueryResult> queryResults) {
        final List<Integer> matchedLinkIds = Lists.newArrayList();
        final List<Point> snappedPoints = Lists.newArrayList();
        double closestDistance = MAXIMUM_CANDIDATE_DISTANCE_IN_METERS;

        for (final QueryResult queryResult : queryResults) {
            double distance =
                Math.round(queryResult.getQueryDistance() * DISTANCE_SCALE_FACTOR) / DISTANCE_SCALE_FACTOR;
            if (distance <= closestDistance) {

                if (distance < closestDistance) {
                    matchedLinkIds.clear();
                    snappedPoints.clear();
                    closestDistance = distance;
                }

                final IntsRef flags = queryResult.getClosestEdge().getFlags();
                matchedLinkIds.add(flagEncoder.getId(flags));

                GHPoint3D ghSnappedPoint = queryResult.getSnappedPoint();
                Point snappedPoint = geometryFactory.createPoint(
                    new Coordinate(ghSnappedPoint.getLon(), ghSnappedPoint.getLat()));
                if (!snappedPoints.contains(snappedPoint)) {
                    snappedPoints.add(snappedPoint);
                }
            }
        }

        return new SinglePointMatch(matchedLinkIds, MatchStatus.MATCH, snappedPoints, closestDistance);
    }

    private SinglePointMatch createFailedMatch() {
        final List<Integer> matchedLinkIds = Lists.newArrayList();
        final List<Point> snappedPoints = Lists.newArrayList();
        final MatchStatus matchStatus = MatchStatus.NO_MATCH;
        final double distance = 0.0;
        return new SinglePointMatch(matchedLinkIds, matchStatus, snappedPoints, distance);
    }
}
