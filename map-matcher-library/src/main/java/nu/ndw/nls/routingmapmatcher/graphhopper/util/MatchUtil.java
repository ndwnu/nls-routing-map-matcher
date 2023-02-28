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

    public static List<QueryResult> getQueryResults(Point point, double radius, LocationIndexTree locationIndexTree,
            EdgeFilter edgeFilter) {
        double latitude = point.getY();
        double longitude = point.getX();

        List<QueryResult> queryResults = locationIndexTree.findNClosest(latitude, longitude, edgeFilter, radius);
        List<QueryResult> candidates = new ArrayList<>(queryResults.size());

        for (QueryResult queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= radius) {
                candidates.add(queryResult);
            }
        }

        return candidates;
    }
}
