package nu.ndw.nls.routingmapmatcher.domain;

import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
public class RoutingMapMatcher {

    private final LineStringMapMatcherFactory lineStringMapMatcherFactory;

    public RoutingMapMatcher(final LineStringMapMatcherFactory lineStringMapMatcherFactory) {
        this.lineStringMapMatcherFactory = lineStringMapMatcherFactory;
    }

    public Stream<LineStringMatch> matchLocations(final RoutingNetwork routingNetwork,
                                                  final MapMatchingRequest mapMatchingRequest) {
        final List<LineStringLocation> locations = mapMatchingRequest.getLocationSupplier().get();
        final int numLocations = locations.size();
        final LineStringMapMatcher lineStringMapMatcher = lineStringMapMatcherFactory
                .createLineStringMapMatcher(routingNetwork);
        final AtomicInteger matched = new AtomicInteger();
        final AtomicInteger processed = new AtomicInteger();
        log.info("Start map matching for {}, count = {}", mapMatchingRequest.getLocationTypeName(), numLocations);
        return locations
                .stream()
                .map(lineStringLocation ->
                        getLineStringMatch(numLocations, lineStringMapMatcher, matched, processed, lineStringLocation)
                );
    }

    private static LineStringMatch getLineStringMatch(final int numLocations, final LineStringMapMatcher lineStringMapMatcher,
                                                      final AtomicInteger matched,
                                                      final AtomicInteger processed,
                                                      final LineStringLocation lineStringLocation) {
        final LineStringMatch match = lineStringMapMatcher.match(lineStringLocation);
        processed.incrementAndGet();
        if (MatchStatus.MATCH.equals(match.getStatus())) {
            matched.incrementAndGet();
        }
        if ((processed.get() + 1) % 100 == 0) {
            log.info("Processed {} of {} total", processed.get() + 1, numLocations);
        }
        if (processed.intValue() == numLocations) {
            log.info("Done. Processed {} locations, {} successfully matched ({}%)", numLocations, matched.get(),
                    matched.get() * 10000 / numLocations / 100.0);
        }
        return match;
    }
}
