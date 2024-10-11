package nu.ndw.nls.routingmapmatcher.isochrone.algorithm;

import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.querygraph.QueryGraphWeightingAdapter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;

@RequiredArgsConstructor
@Getter
public class ShortestPathTreeFactory {

    private static final int MILLISECONDS = 1000;
    private final Weighting defaultWeighting;
    private final EncodingManager encodingManager;

    public IsochroneByTimeDistanceAndWeight createShortestPathTreeByTimeDistanceAndWeight(Weighting weighting,
            QueryGraph queryGraph, TraversalMode traversalMode, double isochroneValue, IsochroneUnit isochroneUnit,
            boolean reverseFlow, boolean reversed, int matchedLinkId) {
        Weighting baseWeighting = weighting == null ? this.defaultWeighting : weighting;
        Weighting queryGraphWeighting = QueryGraphWeightingAdapter.fromQueryGraph(baseWeighting, queryGraph,
                new EdgeIteratorStateReverseExtractor(), reverseFlow != reversed, matchedLinkId, encodingManager);
        IsochroneByTimeDistanceAndWeight isochrone = new IsochroneByTimeDistanceAndWeight(queryGraph,
                queryGraphWeighting, reverseFlow, traversalMode);
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
