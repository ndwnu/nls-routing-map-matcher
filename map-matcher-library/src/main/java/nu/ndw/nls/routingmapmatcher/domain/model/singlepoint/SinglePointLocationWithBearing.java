package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Getter
@ToString(callSuper = true)
public class SinglePointLocationWithBearing extends SinglePointLocation {

    private final Double minBearing;
    private final Double maxBearing;

    public SinglePointLocationWithBearing(int id, Point point, Double minBearing, Double maxBearing, Double radius) {
        super(id, point, radius);
        this.minBearing = minBearing;
        this.maxBearing = maxBearing;
    }
}
