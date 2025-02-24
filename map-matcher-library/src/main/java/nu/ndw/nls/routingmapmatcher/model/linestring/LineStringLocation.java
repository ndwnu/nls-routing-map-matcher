package nu.ndw.nls.routingmapmatcher.model.linestring;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.model.base.BaseLocation;
import org.locationtech.jts.geom.LineString;

@SuperBuilder(toBuilder=true)
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LineStringLocation extends BaseLocation {

    private final int locationIndex;
    private final boolean reversed;
    @ToString.Exclude
    private final LineString geometry;
    private final ReliabilityCalculationType reliabilityCalculationType;
    private final Double radius;
    private final boolean simplifyResponseGeometry;
}
