package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
public class NoExploreLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public NoExploreLimit() {
        super(1);
    }

    @Override
    protected double getLimit(LABEL isochroneLabel, EncodingManager encodingManager) {
        return 0;
    }
}
