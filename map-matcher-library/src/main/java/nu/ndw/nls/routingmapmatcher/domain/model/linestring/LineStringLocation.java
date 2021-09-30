package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import org.locationtech.jts.geom.LineString;

@RequiredArgsConstructor
@Getter
@ToString(callSuper = true)
public class LineStringLocation extends BaseLocation {

    private final int id;
    private final int locationIndex;
    private final boolean reversed;
    private final double lengthInMeters;
    @ToString.Exclude
    private final LineString geometry;
    private final ReliabilityCalculationType reliabilityCalculationType;

    public LineStringLocation(final int id, final int locationIndex, final boolean reversed,
            final double lengthInMeters, final LineString geometry,
            final ReliabilityCalculationType reliabilityCalculationType, final double upstreamIsochrone,
            final IsochroneUnit upstreamIsochroneUnit, final double downstreamIsochrone,
            final IsochroneUnit downstreamIsochroneUnit) {
        super(upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.id = id;
        this.locationIndex = locationIndex;
        this.reversed = reversed;
        this.lengthInMeters = lengthInMeters;
        this.geometry = geometry;
        this.reliabilityCalculationType = reliabilityCalculationType;
    }
}
