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
    private final Double cutoffDistance;
    private final BearingFilter bearingFilter;

    public SinglePointLocation(final int id, final Point point, final double upstreamIsochrone,
            final IsochroneUnit upstreamIsochroneUnit, final double downstreamIsochrone,
            final IsochroneUnit downstreamIsochroneUnit) {
        super(id, upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.point = point;
        this.cutoffDistance = null;
        this.bearingFilter = null;
    }

    public SinglePointLocation(final int id, final Point point, final Double cutoffDistance,
            final BearingFilter bearingFilter) {
        super(id);
        this.point = point;
        this.cutoffDistance = cutoffDistance;
        this.bearingFilter = bearingFilter;
    }
}
