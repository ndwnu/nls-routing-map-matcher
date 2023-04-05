package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import static java.util.Comparator.comparing;

import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchSort {
    HIGHEST_RELIABILITY(comparing(MatchedPoint::getReliability).reversed()),
    SHORTEST_DISTANCE(comparing(MatchedPoint::getDistance));
    private final Comparator<MatchedPoint> sort;

}
