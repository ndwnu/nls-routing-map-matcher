package nu.ndw.nls.routingmapmatcher.model.singlepoint;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedPoint {

    private final int matchedLinkId;
    private final int matchedReversedLinkId;
    private final boolean reversed;
    private final Point snappedPoint;
    private final double fraction;
    private final double distance;
    private final double bearing;
    private final double reliability;

    public int getLinkIdInDirection() {
        return isReversed() && hasReversedLinkId() ? getMatchedReversedLinkId() : getMatchedLinkId();
    }

    public boolean hasReversedLinkId() {
        return getMatchedReversedLinkId() > 0;
    }
}
