package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import java.util.List;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import org.locationtech.jts.geom.LineString;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LineStringMatch extends MapMatch {

    private final int locationIndex;
    private final boolean reversed;
    private final List<Integer> matchedLinkIds;
    private final Set<Integer> upstreamLinkIds;
    private final Set<Integer> downstreamLinkIds;
    private final double startLinkFraction;
    private final double endLinkFraction;
    @ToString.Exclude
    private final LineString lineString;
}
