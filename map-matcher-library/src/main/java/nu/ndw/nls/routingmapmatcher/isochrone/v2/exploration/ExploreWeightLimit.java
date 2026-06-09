package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.IsoLabel;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
public class ExploreWeightLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public ExploreWeightLimit(double limit) {
        super(limit);
    }

    @Override
    public double getLimit(LABEL isochroneLabel, EncodingManager encodingManager) {
        return isochroneLabel.getWeight();
    }
}
