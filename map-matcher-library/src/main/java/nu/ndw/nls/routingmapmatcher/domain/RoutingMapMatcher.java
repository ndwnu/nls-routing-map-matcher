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
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;

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

        public Stream<R> matchLocations(MapMatchingRequest<T> mapMatchingRequest) {
            List<T> locations = mapMatchingRequest.getLocationSupplier().get();

            int numLocations = locations.size();
            log.info("Start map matching for {}, count = {}", mapMatchingRequest.getLocationTypeName(), numLocations);

            return locations.stream().map(location -> this.performMatching(numLocations, location));
        }

        private R performMatching(int numLocations, T location) {
            R match = this.mapMatcher.match(location);
            this.processed++;
            if (match.getStatus() == MatchStatus.MATCH) {
                this.matched++;
            }
            if (this.processed % REPORT_PROGRESS_INTERVAL == 0) {
                log.info("Processed {} of {} total", this.processed, numLocations);
            }
            if (this.processed == numLocations) {
                double percentage = HUNDRED_PERCENT * this.matched / numLocations;
                log.info("Done. Processed {} locations, {} successfully matched ({}%)", numLocations, this.matched,
                        String.format(Locale.getDefault(), "%.2f", percentage));
            }

            return match;
        }
    }

    public Stream<SinglePointMatch> matchLocations(RoutingNetwork routingNetwork,
            MapMatchingSinglePointRequest mapMatchingSinglePointRequest) {
        SinglePointMapMatcher singlePointMapMatcher =
                this.singlePointMapMatcherMapMatcherFactory.createMapMatcher(routingNetwork);

        return matchLocations(singlePointMapMatcher, mapMatchingSinglePointRequest);
    }

    public Stream<SinglePointMatch> matchLocations(NetworkGraphHopper preInitializedNetwork,
            MapMatchingSinglePointRequest mapMatchingSinglePointRequest) {
        SinglePointMapMatcher singlePointMapMatcher =
                this.singlePointMapMatcherMapMatcherFactory.createMapMatcher(preInitializedNetwork);

        return matchLocations(singlePointMapMatcher, mapMatchingSinglePointRequest);
    }

    public Stream<LineStringMatch> matchLocations(RoutingNetwork routingNetwork,
            MapMatchingLineRequest mapMatchingLineRequest) {
        MapMatcher<LineStringLocation, LineStringMatch> lineStringMapMatcher =
                mapMatchingLineRequest.getLineMatchingMode() == LineMatchingMode.LINE_STRING
                        ? this.lineStringMapMatcherFactory.createMapMatcher(routingNetwork)
                        : this.startToEndMapMatcherMapMatcherFactory.createMapMatcher(routingNetwork);

        return matchLocations(lineStringMapMatcher, mapMatchingLineRequest);
    }

    public Stream<LineStringMatch> matchLocations(NetworkGraphHopper preInitializedNetwork,
            MapMatchingLineRequest mapMatchingLineRequest) {
        MapMatcher<LineStringLocation, LineStringMatch> lineStringMapMatcher =
                mapMatchingLineRequest.getLineMatchingMode() == LineMatchingMode.LINE_STRING
                        ? this.lineStringMapMatcherFactory.createMapMatcher(preInitializedNetwork)
                        : this.startToEndMapMatcherMapMatcherFactory.createMapMatcher(preInitializedNetwork);

        return matchLocations(lineStringMapMatcher, mapMatchingLineRequest);
    }

    private <T extends BaseLocation, R extends MapMatch> Stream<R> matchLocations(MapMatcher<T, R> mapMatcher,
            MapMatchingRequest<T> mapMatchingRequest) {
        MatchingContext<T, R> matchingContext = new MatchingContext<>(mapMatcher);

        return matchingContext.matchLocations(mapMatchingRequest);
    }
}
