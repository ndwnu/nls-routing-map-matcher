package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@Getter
@ToString(callSuper = true)
public class SinglePointLocationWithBearing extends SinglePointLocation {

    private final BearingRange bearingRange;

    public SinglePointLocationWithBearing(int id, Point point, BearingRange bearingRange, Double radius) {
        super(id, point, radius);
        this.bearingRange = bearingRange;
    }
}
