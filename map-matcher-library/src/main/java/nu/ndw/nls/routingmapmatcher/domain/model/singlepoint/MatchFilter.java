package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchFilter {
    ALL(matches -> matches),
    FIRST(matches -> matches.isEmpty() ? emptyList() : List.of(matches.stream().findFirst().get()));

    private final UnaryOperator<List<MatchedPoint>> filter;
}
