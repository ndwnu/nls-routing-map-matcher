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

    public RoutingMapMatcher(LineStringMapMatcherFactory lineStringMapMatcherFactory) {
        this.lineStringMapMatcherFactory = lineStringMapMatcherFactory;
    }

    public Stream<LineStringMatch> matchLocations(RoutingNetwork routingNetwork,
                                                  MapMatchingRequest mapMatchingRequest) {
        final List<LineStringLocation> locations = mapMatchingRequest.getLocationSupplier().get();
        final int numLocations = locations.size();
        final LineStringMapMatcher lineStringMapMatcher = lineStringMapMatcherFactory
                .createLineStringMapMatcher(routingNetwork);
        AtomicInteger matched = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();
        log.info("Start map matching for " + mapMatchingRequest.getName() + ", count = {}", numLocations);
        return locations
                .stream()
                .map(lineStringLocation ->
                        getLineStringMatch(numLocations, lineStringMapMatcher, matched, processed, lineStringLocation)
                );
    }

    private static LineStringMatch getLineStringMatch(int numLocations, LineStringMapMatcher lineStringMapMatcher,
                                                      AtomicInteger matched,
                                                      AtomicInteger processed,
                                                      LineStringLocation lineStringLocation) {
        LineStringMatch match = lineStringMapMatcher.match(lineStringLocation);
        processed.incrementAndGet();
        if (MatchStatus.MATCH.equals(match.getStatus())) {
            matched.incrementAndGet();
            log.info("matched successful {}", matched);
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
