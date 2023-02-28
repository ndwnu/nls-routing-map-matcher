package nu.ndw.nls.routingmapmatcher.graphhopper.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;


@Builder
@Getter
@EqualsAndHashCode
@ToString
public class FractionAndDistance {
    private final double fraction;
    private final double distance;
}
