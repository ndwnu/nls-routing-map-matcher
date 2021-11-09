package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import org.locationtech.jts.geom.LineString;

@Getter
@ToString(callSuper = true)
public class LineStringLocation extends BaseLocation {

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
        super(id, upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone, downstreamIsochroneUnit);
        this.locationIndex = locationIndex;
        this.reversed = reversed;
        this.lengthInMeters = lengthInMeters;
        this.geometry = geometry;
        this.reliabilityCalculationType = reliabilityCalculationType;
    }

    public LineStringLocation(final int id, final int locationIndex, final boolean reversed,
            final double lengthInMeters, final LineString geometry,
            final ReliabilityCalculationType reliabilityCalculationType) {
        super(id);
        this.locationIndex = locationIndex;
        this.reversed = reversed;
        this.lengthInMeters = lengthInMeters;
        this.geometry = geometry;
        this.reliabilityCalculationType = reliabilityCalculationType;
    }
}
