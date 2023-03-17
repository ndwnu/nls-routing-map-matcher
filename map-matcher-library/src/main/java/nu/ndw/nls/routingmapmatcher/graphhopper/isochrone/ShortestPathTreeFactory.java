package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@RequiredArgsConstructor
public class ShortestPathTreeFactory {

    private static final int MILLISECONDS = 1000;
    private final Weighting weighting;

    public ShortestPathTree createShortestPathtree(QueryGraph queryGraph, double isochroneValue,
            IsochroneUnit isochroneUnit,
            boolean reverseFlow) {
        ShortestPathTree isochrone = new ShortestPathTree(queryGraph, this.weighting, reverseFlow,
                TraversalMode.NODE_BASED);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue * MILLISECONDS);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
        return isochrone;
    }
}
