package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
@ToString(callSuper = true)
public class ExploreDistanceLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public ExploreDistanceLimit(double limit) {
        this(limit, false);
    }

    public ExploreDistanceLimit(double limit, boolean applyLimitToParent) {
        super(limit, applyLimitToParent);
    }

    @Override
    public double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager) {
        return isochroneLabel.getDistance();
    }

    @Override
    public String debug(LABEL isochroneLabel, EncodingManager encodingManager) {
        return "ExploreDistanceLimit{limit=%s, distance=%s, reached=%s}".formatted(
                getLimit(),
                getValueForLabel(isochroneLabel, encodingManager),
                !isInLimit(isochroneLabel, encodingManager)
        );
    }
}
