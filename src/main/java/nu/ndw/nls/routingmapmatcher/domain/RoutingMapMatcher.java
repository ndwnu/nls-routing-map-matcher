package nu.ndw.nls.routingmapmatcher.domain;

import com.google.common.base.Supplier;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class RoutingMapMatcher {

    private final LineStringMapMatcherFactory lineStringMapMatcherFactory;

    private static final Logger logger = LoggerFactory.getLogger(RoutingMapMatcher.class);

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
        return locations
                .stream().map(lineStringLocation ->
                        getLineStringMatch(numLocations, lineStringMapMatcher, matched, processed, lineStringLocation)
                );
    }

    private static LineStringMatch getLineStringMatch(int numLocations, LineStringMapMatcher lineStringMapMatcher,
                                                      AtomicInteger matched,
                                                      AtomicInteger processed,
                                                      LineStringLocation lineStringLocation) {
        LineStringMatch match = lineStringMapMatcher.match(lineStringLocation);
        processed.getAndIncrement();
        if (MatchStatus.MATCH.equals(match.getStatus())) {
            matched.getAndIncrement();
        }
        if ((processed.get() + 1) % 100 == 0) {
            logger.info("Processed {} of {} total", processed.get() + 1, numLocations);
        }
        return match;
    }
}
