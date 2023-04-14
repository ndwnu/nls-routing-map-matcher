package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.LineString;


@Builder
@Getter
@EqualsAndHashCode
@ToString
public class IsochroneMatch {

    private final int matchedLinkId;
    @ToString.Exclude
    private final LineString geometry;
    private final double startFraction;
    private final double endFraction;
    private final boolean reversed;
}
