package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
public class ExploreTimeLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL>{

    public ExploreTimeLimit(double limit) {
        super(limit);
    }

    @Override
    public double getLimit(LABEL isochroneLabel, EncodingManager encodingManager) {
        return isochroneLabel.getTime();
    }
}
