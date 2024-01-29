package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchSort;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class SinglePointMapMatcherWithIsochroneIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private static final Coordinate START_POINT = new Coordinate(5.4267250, 52.1767242);
    private static final int UPSTREAM_ISOCHRONE_METERS = 200;
    private static final BearingFilter BEARING_FILTER = new BearingFilter(135, 10);
    private static final int CUTOFF_DISTANCE = 20;
    private static final int ID = 1;

    private SinglePointMapMatcher singlePointMapMatcher;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        singlePointMapMatcher = new SinglePointMapMatcher(TestNetworkProvider.getTestNetworkFromFile(LINKS_RESOURCE), CAR_FASTEST);
        geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
    }

    @SneakyThrows
    @Test
    void matchWithDownstreamIsochrone_Meters_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .downstreamIsochrone(UPSTREAM_ISOCHRONE_METERS)
                .bearingFilter(BEARING_FILTER)
                .downstreamIsochroneUnit(IsochroneUnit.METERS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatch match = result.getCandidateMatches().get(0);
        assertThat(match.getDownstream(), hasSize(5));
        var startPoint = match.getDownstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.673088825759));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertFalse(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void matchWithDownstreamIsochrone_Seconds_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .downstreamIsochrone(20)
                .bearingFilter(BEARING_FILTER)
                .downstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatch match = result.getCandidateMatches().get(0);
        assertThat(match.getDownstream(), hasSize(14));
        var startPoint = match.getDownstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.673088825759));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertFalse(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void matchWithUpstreamIsochrone_Meters_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .upstreamIsochrone(UPSTREAM_ISOCHRONE_METERS)
                .bearingFilter(BEARING_FILTER)
                .upstreamIsochroneUnit(IsochroneUnit.METERS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatch match = result.getCandidateMatches().get(0);
        assertThat(match.getUpstream(), hasSize(5));
        var startPoint = match.getUpstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.326911174226));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertTrue(startPoint.isReversed());
    }

    @SneakyThrows
    @Test
    void matchWithUpstreamIsochrone_Seconds_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(START_POINT);
        var request = SinglePointLocation
                .builder()
                .id(ID)
                .point(point)
                .matchSort(MatchSort.SHORTEST_DISTANCE)
                .matchFilter(MatchFilter.FIRST)
                .upstreamIsochrone(20)
                .bearingFilter(BEARING_FILTER)
                .upstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .cutoffDistance(CUTOFF_DISTANCE)
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result.getCandidateMatches(), hasSize(ID));
        CandidateMatch match = result.getCandidateMatches().get(0);
        assertThat(match.getUpstream(), hasSize(11));
        var startPoint = match.getUpstream().get(0);
        assertThat(startPoint.getStartFraction(), is(0.326911174226));
        assertThat(startPoint.getEndFraction(), is(1.0));
        assertTrue(startPoint.isReversed());
    }
}
