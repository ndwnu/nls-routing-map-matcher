package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@SuppressWarnings("java:S119")
@ToString(callSuper = true)
public class ExploreWeightLimit<LABEL extends IsochroneLabel> extends ExploreLimit<LABEL> {

    public ExploreWeightLimit(double limit) {
        this(limit, false);
    }

    public ExploreWeightLimit(double limit, boolean applyLimitToParent) {
        super(limit, applyLimitToParent);
    }

    @Override
    public double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager) {
        return isochroneLabel.getWeight();
    }

    @Override
    public String debug(LABEL isochroneLabel, EncodingManager encodingManager) {
        return "ExploreWeightLimit{limit=%s, weight=%s, reached=%s}".formatted(
                getLimit(),
                getValueForLabel(isochroneLabel, encodingManager),
                !isInLimit(isochroneLabel, encodingManager)
        );
    }
}
