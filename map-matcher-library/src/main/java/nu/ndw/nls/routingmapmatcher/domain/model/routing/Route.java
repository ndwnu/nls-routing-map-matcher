package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.LineString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class Route {

    private final double startFraction;
    private final double endFraction;

    private List<Integer> matchedLinkIds;

    private final LineString geometry;
    private final double weight;
    private final long duration;
    private final double distance;
}
