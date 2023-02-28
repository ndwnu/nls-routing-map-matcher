package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import org.locationtech.jts.geom.LineString;


@Builder
@Getter
@EqualsAndHashCode
@ToString
public class IsochroneMatch {

    public enum Direction {FORWARD, BACKWARD}

    private final int matchedLinkId;
    @ToString.Exclude
    private final LineString geometry;
    private final Double startFraction;
    private final Double endFraction;
    private final Direction direction;
}
