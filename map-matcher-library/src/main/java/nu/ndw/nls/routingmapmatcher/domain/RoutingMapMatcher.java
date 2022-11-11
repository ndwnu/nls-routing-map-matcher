package nu.ndw.nls.routingmapmatcher.domain;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.LineMatchingMode;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingLineRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MapMatchingSinglePointRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;

@Slf4j
@RequiredArgsConstructor
public class RoutingMapMatcher {

    private static final int REPORT_PROGRESS_INTERVAL = 100;
    private static final double HUNDRED_PERCENT = 100.0;

    private final MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory;
    private final MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherMapMatcherFactory;
    private final MapMatcherFactory<StartToEndMapMatcher> startToEndMapMatcherMapMatcherFactory;

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private static final class MatchingContext<T extends BaseLocation, R extends MapMatch> {

        private int matched;
        private int processed;

        private final MapMatcher<T, R> mapMatcher;

        public Stream<R> matchLocations(final MapMatchingRequest<T> mapMatchingRequest) {
            final List<T> locations = mapMatchingRequest.getLocationSupplier().get();

            final int numLocations = locations.size();
            log.info("Start map matching for {}, count = {}", mapMatchingRequest.getLocationTypeName(), numLocations);

            return locations.stream().map(location -> this.performMatching(numLocations, location));
        }

        private R performMatching(final int numLocations, final T location) {
            final R match = this.mapMatcher.match(location);
            this.processed++;
            if (match.getStatus() == MatchStatus.MATCH) {
                this.matched++;
            }
            if (this.processed % REPORT_PROGRESS_INTERVAL == 0) {
                log.info("Processed {} of {} total", this.processed, numLocations);
            }
            if (this.processed == numLocations) {
                final double percentage = HUNDRED_PERCENT * this.matched / numLocations;
                log.info("Done. Processed {} locations, {} successfully matched ({}%)", numLocations, this.matched,
                        String.format(Locale.getDefault(), "%.2f", percentage));
            }

            return match;
        }
    }

    public Stream<SinglePointMatch> matchLocations(final RoutingNetwork routingNetwork,
            final MapMatchingSinglePointRequest mapMatchingSinglePointRequest) {
        final SinglePointMapMatcher singlePointMapMatcher =
                this.singlePointMapMatcherMapMatcherFactory.createMapMatcher(routingNetwork);

        final MatchingContext<SinglePointLocation, SinglePointMatch> lineStringMapMatchingContext =
                new MatchingContext<>(singlePointMapMatcher);

        return lineStringMapMatchingContext.matchLocations(mapMatchingSinglePointRequest);
    }

    public Stream<LineStringMatch> matchLocations(final RoutingNetwork routingNetwork,
            final MapMatchingLineRequest mapMatchingLineRequest) {
        final MapMatcher<LineStringLocation, LineStringMatch> lineStringMapMatcher =
                mapMatchingLineRequest.getLineMatchingMode() == LineMatchingMode.LINE_STRING
                        ? this.lineStringMapMatcherFactory.createMapMatcher(routingNetwork)
                        : this.startToEndMapMatcherMapMatcherFactory.createMapMatcher(routingNetwork);

        final MatchingContext<LineStringLocation, LineStringMatch> lineStringMapMatchingContext =
                new MatchingContext<>(lineStringMapMatcher);

        return lineStringMapMatchingContext.matchLocations(mapMatchingLineRequest);
    }
}
