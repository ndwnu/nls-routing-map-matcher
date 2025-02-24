package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.assertj.core.api.Assertions.assertThat;

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
class SinglePointMapMatcherWithBidirectionalIsochroneIT {

    private static final String LINKS_RESOURCE = "/test-data/network_bidirectional.geojson";
    private static final Coordinate START_POINT = new Coordinate(5.4303030, 52.1804201);
    private static final int ISOCHRONE_METERS = 100;
    private static final BearingFilter BEARING_FILTER_FORWARD = new BearingFilter(105, 10);
    private static final BearingFilter BEARING_FILTER_REVERSE = new BearingFilter(285, 10);
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
    void match_ok_bidirectionalForwardDownstream() {
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
                .bearingFilter(BEARING_FILTER_FORWARD)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getDownstream()).hasSize(4);
        // Here, we expect the one-way segments not to be matched, because they are in the wrong driving direction.
        assertThat(match.getDownstream()).map(IsochroneMatch::getMatchedLinkId)
                .containsExactly(6405235, 6405226, 6405225, 6405227);
        assertThat(match.getDownstream()).noneMatch(IsochroneMatch::isReversed);
    }

    @SneakyThrows
    @Test
    void match_ok_bidirectionalReverseUpstream() {
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
                .bearingFilter(BEARING_FILTER_REVERSE)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getUpstream()).hasSize(6);
        // Here, we expect the one-way segments to be matched, because they are in the correct driving direction.
        assertThat(match.getUpstream()).map(IsochroneMatch::getMatchedLinkId)
                .containsExactly(6405234, 6405225, 6405239, 6405224, 6405226, 6405238);
        assertThat(match.getUpstream()).noneMatch(IsochroneMatch::isReversed);
    }

    @SneakyThrows
    @Test
    void match_ok_bidirectionalReverseDownstream() {
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
                .bearingFilter(BEARING_FILTER_REVERSE)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getDownstream()).hasSize(7);
        assertThat(match.getDownstream()).map(IsochroneMatch::getMatchedLinkId)
                .containsExactly(6405234, 6405240, 6405251, 6405241, 6405247, 6405250, 6405252);
        assertThat(match.getDownstream()).noneMatch(IsochroneMatch::isReversed);
    }

    @SneakyThrows
    @Test
    void match_ok_bidirectionalForwardUpstream() {
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
                .bearingFilter(BEARING_FILTER_FORWARD)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches()).hasSize(1);
        CandidateMatch match = result.getCandidateMatches().getFirst();
        assertThat(match.getUpstream()).hasSize(7);
        // These are the same 7 segments as in bidirectionalReverseDownstream, but each their reverse link ID.
        assertThat(match.getUpstream()).map(IsochroneMatch::getMatchedLinkId)
                .containsExactly(6405235, 6405247, 6405250, 6405246, 6405240, 6405251, 6405249);
        assertThat(match.getUpstream()).noneMatch(IsochroneMatch::isReversed);
    }
}
