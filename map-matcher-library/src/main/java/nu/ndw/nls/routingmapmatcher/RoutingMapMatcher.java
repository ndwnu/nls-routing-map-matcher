package nu.ndw.nls.routingmapmatcher;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.model.LineMatchingMode;
import nu.ndw.nls.routingmapmatcher.model.MapMatchingLineRequest;
import nu.ndw.nls.routingmapmatcher.model.MapMatchingRequest;
import nu.ndw.nls.routingmapmatcher.model.MapMatchingSinglePointRequest;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.model.base.MapMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.singlepoint.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.starttoend.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.viterbi.ViterbiLineStringMapMatcher;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class RoutingMapMatcher {

    private static final int REPORT_PROGRESS_INTERVAL = 100;
    private static final double HUNDRED_PERCENT = 100.0;

    private final MapMatcherFactory<ViterbiLineStringMapMatcher> lineStringMapMatcherFactory;
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

    public Stream<SinglePointMatch> matchLocations(NetworkGraphHopper preInitializedNetwork,
            MapMatchingSinglePointRequest mapMatchingSinglePointRequest, String profileName) {
        SinglePointMapMatcher singlePointMapMatcher =
                this.singlePointMapMatcherMapMatcherFactory.createMapMatcher(preInitializedNetwork, profileName);

        return matchLocations(singlePointMapMatcher, mapMatchingSinglePointRequest);
    }

    public Stream<LineStringMatch> matchLocations(NetworkGraphHopper preInitializedNetwork,
            MapMatchingLineRequest mapMatchingLineRequest, String profileName) {

        MapMatcher<LineStringLocation, LineStringMatch> lineStringMapMatcher =
                getFactory(mapMatchingLineRequest.getLineMatchingMode())
                        .createMapMatcher(preInitializedNetwork, profileName);

        return matchLocations(lineStringMapMatcher, mapMatchingLineRequest);
    }

    private <T extends BaseLocation, R extends MapMatch> Stream<R> matchLocations(MapMatcher<T, R> mapMatcher,
            MapMatchingRequest<T> mapMatchingRequest) {
        MatchingContext<T, R> matchingContext = new MatchingContext<>(mapMatcher);

        return matchingContext.matchLocations(mapMatchingRequest);
    }

    private MapMatcherFactory<? extends MapMatcher<LineStringLocation, LineStringMatch>> getFactory(
            LineMatchingMode lineMatchingMode) {
        return switch (Objects.requireNonNull(lineMatchingMode)) {
            case LINE_STRING -> this.lineStringMapMatcherFactory;
            case START_TO_END -> this.startToEndMapMatcherMapMatcherFactory;
        };
    }

}
