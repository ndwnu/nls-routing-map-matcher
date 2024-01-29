package nu.ndw.nls.routingmapmatcher.model;

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
    private final double fractionDistance;
    private final double totalDistance;
}
