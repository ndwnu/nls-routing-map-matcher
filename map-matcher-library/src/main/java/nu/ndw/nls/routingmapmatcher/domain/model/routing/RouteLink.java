package nu.ndw.nls.routingmapmatcher.domain.model.routing;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RouteLink {

    private final Integer linkId;
    private final double startFraction;
    private final double endFraction;
}
