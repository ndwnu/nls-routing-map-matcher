package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import org.locationtech.jts.geom.Point;

@Getter
@ToString(callSuper = true)
public class SinglePointLocation extends BaseLocation {

    private final Point point;
    private final Double radius;

    public SinglePointLocation(final int id, final Point point, final double upstreamIsochrone,
            final IsochroneUnit upstreamIsochroneUnit, final double downstreamIsochrone,
            final IsochroneUnit downstreamIsochroneUnit) {
        super(id, upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.point = point;
        this.radius = null;
    }

    public SinglePointLocation(final int id, final Point point) {
        super(id);
        this.point = point;
        this.radius = null;
    }

    public SinglePointLocation(final int id, final Point point, final Double radius) {
        super(id);
        this.point = point;
        this.radius = radius;
    }
}
