package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm;


import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.MUNICIPALITY_CODE;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.util.EdgeIteratorState;


public class IsochroneByMunicipality extends AbstractShortestPathTree {

    private final long municipalityId;
    private final EncodingManager encodingManager;

    public IsochroneByMunicipality(Graph g, Weighting weighting, TraversalMode traversalMode,
            EncodingManager encodingManager,
            int municipalityId
    ) {
        super(g, weighting, false, traversalMode);
        this.encodingManager = encodingManager;
        this.municipalityId = municipalityId;
    }

    @Override
    protected boolean isInLimit(IsoLabel isoLabel) {
        EdgeIteratorState currentEdge = this.graph.getEdgeIteratorState(isoLabel.edge, isoLabel.node);
        IntEncodedValue idEnc = encodingManager.getIntEncodedValue(MUNICIPALITY_CODE.getKey());
        int mCode = currentEdge.get(idEnc);
        return mCode == municipalityId;
    }

}
