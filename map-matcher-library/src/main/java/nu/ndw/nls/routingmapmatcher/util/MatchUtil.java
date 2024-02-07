package nu.ndw.nls.routingmapmatcher.util;

import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.DIST_PLANE;

import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.shapes.Circle;
import java.util.ArrayList;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.locationtech.jts.geom.Point;

public final class MatchUtil {

    private static final int KEY_FACTOR = 2;

    private MatchUtil() {
    }


    public static List<Snap> getQueryResults(NetworkGraphHopper network, Point point, double radius,
            LocationIndexTree locationIndexTree, EdgeFilter edgeFilter) {
        double latitude = point.getY();
        double longitude = point.getX();
        Circle circle = new Circle(latitude, longitude, radius);
        List<Snap> queryResults = new ArrayList<>();

        locationIndexTree.query(circle.getBounds(), edgeId -> {
            EdgeIteratorState edge = network.getBaseGraph()
                    .getEdgeIteratorStateForKey(edgeId * KEY_FACTOR);
            var geometry = edge.fetchWayGeometry(FetchMode.ALL).makeImmutable();
            if (circle.intersects(geometry) && edgeFilter.accept(edge)) {
                var snap = new Snap(latitude, longitude);
                locationIndexTree
                        .traverseEdge(latitude, longitude, edge,
                                (node, normedDist, wayIndex, pos) -> {
                                    if (normedDist < snap.getQueryDistance()) {
                                        snap.setQueryDistance(normedDist);
                                        snap.setClosestNode(node);
                                        snap.setClosestEdge(edge.detach(false));
                                        snap.setWayIndex(wayIndex);
                                        snap.setSnappedPosition(pos);
                                    }
                                });
                if (snap.isValid()) {
                    snap.setQueryDistance(DIST_PLANE.calcDenormalizedDist(snap.getQueryDistance()));
                    snap.calcSnappedPoint(DIST_PLANE);
                    queryResults.add(snap);
                }
            }
        });
        List<Snap> candidates = new ArrayList<>();
        for (Snap queryResult : queryResults) {
            if (queryResult.getQueryDistance() <= radius) {
                candidates.add(queryResult);
            }
        }

        return candidates;
    }
}
