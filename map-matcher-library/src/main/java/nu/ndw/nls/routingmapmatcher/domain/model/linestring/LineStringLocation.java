package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import org.locationtech.jts.geom.LineString;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LineStringLocation extends BaseLocation {

    private final int locationIndex;
    private final boolean reversed;
    private final Double lengthInMeters;
    @ToString.Exclude
    private final LineString geometry;
    private final ReliabilityCalculationType reliabilityCalculationType;
    private final Double radius;
    @Builder.Default
    private final boolean simplifyResponseGeometry = false;
}
