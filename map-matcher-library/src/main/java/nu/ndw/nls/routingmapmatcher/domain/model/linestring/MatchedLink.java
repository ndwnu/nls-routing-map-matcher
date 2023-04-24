package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedLink {
    private final int linkId;
    private final boolean reversed;
}
