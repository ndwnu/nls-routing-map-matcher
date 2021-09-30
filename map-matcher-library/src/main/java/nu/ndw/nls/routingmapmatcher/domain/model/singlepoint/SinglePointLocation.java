package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import org.locationtech.jts.geom.Point;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@RequiredArgsConstructor
@Getter
@ToString(callSuper = true)
public class SinglePointLocation extends BaseLocation {

    private final Point point;

    public SinglePointLocation(final Point point, final double upstreamIsochrone,
            final IsochroneUnit upstreamIsochroneUnit, final double downstreamIsochrone,
            final IsochroneUnit downstreamIsochroneUnit) {
        super(upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.point = point;
    }
}
