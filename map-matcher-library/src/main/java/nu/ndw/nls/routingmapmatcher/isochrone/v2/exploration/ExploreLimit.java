package nu.ndw.nls.routingmapmatcher.isochrone.v2.exploration;

import com.graphhopper.routing.util.EncodingManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.isochrone.v2.dto.IsochroneLabel;

@RequiredArgsConstructor
@SuppressWarnings("java:S119")
@Slf4j
@ToString
public abstract class ExploreLimit<LABEL extends IsochroneLabel> {

    @Getter(AccessLevel.PROTECTED)
    private final double limit;

    @Getter(AccessLevel.PROTECTED)
    private final boolean applyLimitToParent;

    public boolean isInLimit(LABEL isochroneLabel, EncodingManager encodingManager) {
        if (applyLimitToParent && isochroneLabel.isRoot()) {
            return true;
        }

        return (this.limit - getValueForLabel(
                getLabelToUse(isochroneLabel),
                encodingManager)) > 0;
    }

    protected abstract double getValueForLabel(LABEL isochroneLabel, EncodingManager encodingManager);

    public abstract String debug(LABEL isochroneLabel, EncodingManager encodingManager);

    protected LABEL getLabelToUse(LABEL isochroneLabel) {
        return applyLimitToParent ? isochroneLabel.getParent() : isochroneLabel;
    }
}
