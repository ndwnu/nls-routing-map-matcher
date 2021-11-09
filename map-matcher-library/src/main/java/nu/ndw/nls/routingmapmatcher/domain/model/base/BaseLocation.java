package nu.ndw.nls.routingmapmatcher.domain.model.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@RequiredArgsConstructor
@Getter
@ToString
public abstract class BaseLocation {

    private final int id;

    private final double upstreamIsochrone;
    private final IsochroneUnit upstreamIsochroneUnit;
    private final double downstreamIsochrone;
    private final IsochroneUnit downstreamIsochroneUnit;

    protected BaseLocation(final int id) {
        this.id = id;
        upstreamIsochrone = 0;
        upstreamIsochroneUnit = null;
        downstreamIsochrone = 0;
        downstreamIsochroneUnit = null;
    }
}
