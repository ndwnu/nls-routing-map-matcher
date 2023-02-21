package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.QueryResult;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Point;

public final class MatchUtil {

    private MatchUtil() {

    }

    public static List<QueryResult> getQueryResults(final Point point, final double radius,
            final LocationIndexTree locationIndexTree, final EdgeFilter edgeFilter) {
        final double latitude = point.getY();
        final double longitude = point.getX();

        final List<QueryResult> queryResults = locationIndexTree.findNClosest(latitude, longitude, edgeFilter, radius);
        final List<QueryResult> candidates = new ArrayList<>(queryResults.size());

        for (final QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= radius) {
                candidates.add(queryResult);
            }
        }

        return candidates;
    }
}
