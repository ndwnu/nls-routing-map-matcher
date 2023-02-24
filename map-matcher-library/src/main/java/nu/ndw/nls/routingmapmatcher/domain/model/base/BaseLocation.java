package nu.ndw.nls.routingmapmatcher.domain.model.base;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public abstract class BaseLocation {

    private final int id;

    private final double upstreamIsochrone;
    private final IsochroneUnit upstreamIsochroneUnit;
    private final double downstreamIsochrone;
    private final IsochroneUnit downstreamIsochroneUnit;
}
