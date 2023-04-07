package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchFilter {
    ALL,
    FIRST;
}
