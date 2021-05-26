package nu.ndw.nls.routingmapmatcher.domain;

import com.google.common.base.Supplier;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
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
                                                  Supplier<List<LineStringLocation>> locationsSupplier) {
        final List<LineStringLocation> locations = locationsSupplier.get();
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

    private static LineStringMatch getLineStringMatch(int numLocations, LineStringMapMatcher lineStringMapMatcher, AtomicInteger matched, AtomicInteger processed, LineStringLocation lineStringLocation) {
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

   /* public void matchLocations(final int networkVersion, final int mapVersion, final String locationsName) {
        final List<LineStringLocation> locations = lineStringLocationRepository.getLocations(mapVersion);
        final int numLocations = locations.size();
        logger.info("Start map matching for " + locationsName + ", count = {}", numLocations);
        final Stopwatch stopwatch = Stopwatch.createStarted();
        final LineStringMapMatcher lineStringMapMatcher = lineStringMapMatcherFactory
                .createLineStringMapMatcher(networkVersion);
        final List<LineStringMatch> matches = new ArrayList<>(numLocations);
        int matched = 0;
        for (int i = 0; i < numLocations; i++) {
            final LineStringMatch match = lineStringMapMatcher.match(locations.get(i));
            matches.add(match);
            if (MatchStatus.MATCH.equals(match.getStatus())) {
                matched++;
            }
            if ((i + 1) % 100 == 0) {
                logger.info("Processed {} " + locationsName + " of {} total", i + 1, numLocations);
            }
        }

        logger.info("Writing results to database. Processing took {}", stopwatch);
        lineStringLocationRepository.replaceMatches(matches);
        logger.info("Done. Processed {} locations, {} successfully matched ({}%)", numLocations, matched,
                matched * 10000 / numLocations / 100.0);
    }*/


}
