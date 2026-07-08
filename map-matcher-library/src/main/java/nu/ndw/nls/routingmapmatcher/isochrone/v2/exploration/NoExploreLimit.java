package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
@ToString(callSuper = true)
public class NoExploreLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public static final int LIMIT = 1;

    public static final int ACCESSIBLE = 0;

    public NoExploreLimit() {
        super(LIMIT, false);
    }

    @Override
    protected double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager) {
        return ACCESSIBLE;
    }

    @Override
    public String debug(LABEL isochroneLabel, EncodingManager encodingManager) {
        return "NoExploreLimit{reached=false}";
    }
}
