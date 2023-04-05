package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingDouble;

import java.util.Comparator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MatchSort {
    HIGHEST_RELIABILITY(comparingDouble(MatchedPoint::getReliability).reversed()),
    SHORTEST_DISTANCE(comparingDouble(MatchedPoint::getDistance));
    private final Comparator<MatchedPoint> sort;

}
