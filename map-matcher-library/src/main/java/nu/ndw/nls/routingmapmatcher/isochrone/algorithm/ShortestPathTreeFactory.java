package nu.ndw.nls.routingmapmatcher.isochrone.algorithm;

import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;

@RequiredArgsConstructor
public class ShortestPathTreeFactory {

    private static final int MILLISECONDS = 1000;
    private final Weighting defaultWeighting;

    public IsochroneByTimeDistanceAndWeight createShortestPathTreeByTimeDistanceAndWeight(Weighting weighting,
            QueryGraph queryGraph,
            TraversalMode traversalMode,
            double isochroneValue,
            IsochroneUnit isochroneUnit,
            boolean reverseFlow
    ) {
        IsochroneByTimeDistanceAndWeight isochrone = new IsochroneByTimeDistanceAndWeight(queryGraph,
                weighting == null ? this.defaultWeighting : weighting, reverseFlow,
                traversalMode);
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
