package nu.ndw.nls.routingmapmatcher.domain.model.base;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public abstract class MapMatch {

    private final int id;
    private final MatchStatus status;
    private final double reliability;
}
