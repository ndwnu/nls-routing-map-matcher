package nu.ndw.nls.routingmapmatcher.domain.model.base;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;

@ToString
@Getter
@RequiredArgsConstructor
public class MapMatch {

    private final int id;
    private final MatchStatus status;
    private final double reliability;


}
