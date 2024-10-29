package nu.ndw.nls.routingmapmatcher.model.linestring;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedEdgeLink {

    private final int linkId;
    private final boolean reversed;
    private final double distance;
}
