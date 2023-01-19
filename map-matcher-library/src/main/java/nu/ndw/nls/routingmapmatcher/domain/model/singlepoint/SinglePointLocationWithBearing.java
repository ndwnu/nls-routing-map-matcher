package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Getter
@ToString(callSuper = true)
public class SinglePointLocationWithBearing extends SinglePointLocation {
    private final List<Double> bearings;
    private final Double radius;

    public SinglePointLocationWithBearing(int id, Point point, List<Double> bearings, Double radius) {
        super(id, point);
        this.bearings = bearings;
        this.radius = radius;
    }
}
