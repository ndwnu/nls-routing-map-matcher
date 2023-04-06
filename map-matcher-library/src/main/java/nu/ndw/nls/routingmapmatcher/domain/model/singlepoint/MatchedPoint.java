package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.Direction;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedPoint {

    private final int matchedLinkId;
    //private final boolean reversed;
    private Direction direction;
    private final Point snappedPoint;
    private final double fraction;
    private final double distance;
    private final double bearing;
    private final double reliability;
}
