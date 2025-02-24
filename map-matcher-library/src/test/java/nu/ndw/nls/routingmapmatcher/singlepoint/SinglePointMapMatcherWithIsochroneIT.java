package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.junit.jupiter.api.Assertions.assertFalse;

import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchSort;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class SinglePointMapMatcherWithIsochroneIT {

    private static final String LINKS_RESOURCE = "/test-data/network_bidirectional.geojson";
    private static final Coordinate START_POINT = new Coordinate(5.4267250, 52.1767242);
    private static final int ISOCHRONE_METERS = 200;
    private static final int ISOCHRONE_SECONDS = 20;
    private static final BearingFilter BEARING_FILTER = new BearingFilter(140, 10);
    private static final int CUTOFF_DISTANCE = 20;
    private static final int ID = 1;

    @Autowired
    private SinglePointMapMatcherFactory singlePointMapMatcherFactory;
    @Autowired
    private GeometryFactoryWgs84 geometryFactory;

    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    private void setupNetwork() {
        singlePointMapMatcher = singlePointMapMatcherFactory.createMapMatcher(
                TestNetworkProvider.getTestNetworkFromFile(LINKS_RESOURCE),
                CAR);
    }

    @SneakyThrows
    @Test
    void match_ok_downstreamIsochroneMeters() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .downstreamIsochrone(ISOCHRONE_METERS)
                .downstreamIsochroneUnit(IsochroneUnit.METERS)
                .bearingFilter(BEARING_FILTER)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getDownstream()).hasSize(5);
        IsochroneMatch startPoint = match.getDownstream().getFirst();
        assertThat(startPoint.getStartFraction()).isCloseTo(match.getFraction(), offset(0.000001));
        assertThat(startPoint.getEndFraction()).isEqualTo(1.0);
        assertFalse(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void match_ok_downstreamIsochroneSeconds() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .downstreamIsochrone(ISOCHRONE_SECONDS)
                .downstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .bearingFilter(BEARING_FILTER)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getDownstream()).hasSize(14);
        IsochroneMatch startPoint = match.getDownstream().getFirst();
        assertThat(startPoint.getStartFraction()).isCloseTo(match.getFraction(), offset(0.000001));
        assertThat(startPoint.getEndFraction()).isEqualTo(1.0);
        assertFalse(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void match_ok_upstreamIsochroneMeters() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .upstreamIsochrone(ISOCHRONE_METERS)
                .upstreamIsochroneUnit(IsochroneUnit.METERS)
                .bearingFilter(BEARING_FILTER)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getUpstream()).hasSize(5);
        IsochroneMatch startPoint = match.getUpstream().getFirst();
        assertThat(startPoint.getStartFraction()).isEqualTo(0.0);
        assertThat(startPoint.getEndFraction()).isCloseTo(match.getFraction(), offset(0.000001));
        assertFalse(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void match_ok_upstreamIsochroneSeconds() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .upstreamIsochrone(ISOCHRONE_SECONDS)
                .upstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .bearingFilter(BEARING_FILTER)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getUpstream()).hasSize(11);
        IsochroneMatch startPoint = match.getUpstream().getFirst();
        assertThat(startPoint.getStartFraction()).isEqualTo(0.0);
        assertThat(startPoint.getEndFraction()).isCloseTo(match.getFraction(), offset(0.000001));
        assertFalse(startPoint.isReversed());
    }
}
