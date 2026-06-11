package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@RequiredArgsConstructor
@SuppressWarnings("java:S119")
public abstract class ExploreLimit<LABEL extends IsochroneLabel> {

    private final double limit;

    public boolean isInLimit(LABEL isochroneLabel, EncodingManager encodingManager) {
        return (this.limit - getLimit(isochroneLabel, encodingManager)) > 0;
    }

    protected abstract double getLimit(LABEL isochroneLabel, EncodingManager encodingManager);
}
