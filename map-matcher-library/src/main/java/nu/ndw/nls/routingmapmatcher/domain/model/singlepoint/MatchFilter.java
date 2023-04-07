package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MatchFilter {
    ALL(matches -> matches),
    FIRST_EQUAL_DISTANCE(matches -> matches.isEmpty() ? emptyList() : filterOnEqual(matches,
            () -> matches.get(0).getDistance(),
            MatchedPoint::getDistance)),
    FIRST_EQUAL_RELIABILITY(matches -> matches.isEmpty() ? emptyList() : filterOnEqual(matches,
            () -> matches.get(0).getReliability(),
            MatchedPoint::getReliability));
    private static final double ROUNDING_ERROR = 0.1;

    private final UnaryOperator<List<MatchedPoint>> filter;

    private static List<MatchedPoint> filterOnEqual(List<MatchedPoint> matches,
            Supplier<Double> firstValueSupplier, Function<MatchedPoint, Double> comparisonProvider) {
        double firstValue = firstValueSupplier.get() + ROUNDING_ERROR;
        return matches.stream().filter(matchedPoint -> comparisonProvider.apply(matchedPoint) < firstValue)
                .toList();
    }
}
