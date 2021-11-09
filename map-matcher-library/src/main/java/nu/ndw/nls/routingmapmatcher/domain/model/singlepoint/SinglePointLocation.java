package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import org.locationtech.jts.geom.Point;

import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@Getter
@ToString(callSuper = true)
public class SinglePointLocation extends BaseLocation {

    private final Point point;

    public SinglePointLocation(final int id, final Point point, final double upstreamIsochrone,
            final IsochroneUnit upstreamIsochroneUnit, final double downstreamIsochrone,
            final IsochroneUnit downstreamIsochroneUnit) {
        super(id, upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.point = point;
    }

    public SinglePointLocation(final int id, final Point point) {
        super(id);
        this.point = point;
    }
}
