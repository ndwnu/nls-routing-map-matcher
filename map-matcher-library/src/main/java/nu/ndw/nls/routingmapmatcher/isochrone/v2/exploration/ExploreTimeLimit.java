package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
@ToString(callSuper = true)
public class ExploreTimeLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public ExploreTimeLimit(double limit) {
        this(limit, false);
    }

    public ExploreTimeLimit(double limit, boolean applyLimitToParent) {
        super(limit, applyLimitToParent);
    }

    @Override
    public double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager) {
        return isochroneLabel.getTime();
    }

    @Override
    public String debug(LABEL isochroneLabel, EncodingManager encodingManager) {
        return "ExploreTimeLimit{limit=%s, time=%s, reached=%s}".formatted(
                getLimit(),
                getValueForLabel(isochroneLabel, encodingManager),
                !isInLimit(isochroneLabel, encodingManager)
        );
    }
}
