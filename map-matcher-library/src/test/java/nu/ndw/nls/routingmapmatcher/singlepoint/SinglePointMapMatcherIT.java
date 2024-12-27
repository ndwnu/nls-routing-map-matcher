package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
public class SinglePointMapMatcherIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private static final String SHIVI_LINKS_RESOURCE = "/test-data/shivi-verkeersbanen.json";
    private static final int ID = 123;
    private static final double DISTANCE_ROUNDING_ERROR = 0.1;
    @Autowired
    private SinglePointMapMatcherFactory singlePointMapMatcherFactory;
    @Autowired
    private GeometryFactoryWgs84 geometryFactory;

    private SinglePointMapMatcher singlePointMapMatcher;


    @SneakyThrows
    private void setupNetwork(String resource) {
        singlePointMapMatcher = singlePointMapMatcherFactory.createMapMatcher(
                TestNetworkProvider.getTestNetworkFromFile(resource), CAR);

    }

    @SneakyThrows
    @ParameterizedTest(name = "{0} [{index}]")
    @CsvFileSource(resources = "/test-data/shivi-flow-mapping-fractions.csv", numLinesToSkip = 1)
    void testFraction(String externalId, double x, double y, double expectedFraction) {
        setupNetwork(SHIVI_LINKS_RESOURCE);

        // The given point is near a one-way road.
        Point point = geometryFactory.createPoint(new Coordinate(x, y));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(1);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getCandidateMatches().getFirst().getFraction()).isCloseTo(expectedFraction, offset(0.01));
    }

    @SneakyThrows
    @Test
    void testOneWayMatch() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is near a one-way road.
        Point point = geometryFactory.createPoint(new Coordinate(5.427, 52.177));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(1);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(66.96798791959225);
    }

    @SneakyThrows
    @Test
    void testBidirectionalWayMatch() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is near a bidirectional road.
        Point point = geometryFactory.createPoint(new Coordinate(5.4280, 52.1798));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(2);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(84.53118547414594);
    }

    @SneakyThrows
    @Test
    void testBidirectionalWayMatchWithBearingFilter() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is near a bidirectional road.
        Point point = geometryFactory.createPoint(new Coordinate(5.4280, 52.1798));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .bearingFilter(new BearingFilter(85, 30))
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(1);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(79.55725120440115);
    }

    @SneakyThrows
    @Test
    void testNodeMatch() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is located at the center of a crossroad.
        Point point = geometryFactory.createPoint(new Coordinate(5.426228, 52.18103));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(8);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(100.0);
    }

    @SneakyThrows
    @Test
    void testNodeMatchWithBearingFilter() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is located at the center of a crossroad.
        Point point = geometryFactory.createPoint(new Coordinate(5.426228, 52.18103));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .bearingFilter(new BearingFilter(15, 30))
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(2);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(97.28721697585911);
    }

    @SneakyThrows
    @Test
    void testDoubleMatch() {
        setupNetwork(LINKS_RESOURCE);

        // The given point is near two one-way roads.
        Point point = geometryFactory.createPoint(new Coordinate(5.424633, 52.178623));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(2);
        assertThatUpstreamAndDownstreamAreNull(candidateMatches);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(2);
        assertThat(singlePointMatch.getReliability()).isEqualTo(95.53279394202733);
    }

    @SneakyThrows
    @Test
    void testNoMatch() {
        setupNetwork(LINKS_RESOURCE);

        Point point = geometryFactory.createPoint(new Coordinate(5.420, 52.190));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.NO_MATCH);
        assertThat(singlePointMatch.getCandidateMatches()).isEmpty();
        assertThat(singlePointMatch.getReliability()).isEqualTo(0.0);
    }

    @SneakyThrows
    @Test
    void testUpstreamDownstream() {
        setupNetwork(LINKS_RESOURCE);

        Point point = geometryFactory.createPoint(new Coordinate(5.4278, 52.1764));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(SinglePointLocation.builder()
                .id(ID)
                .point(point)
                .upstreamIsochrone(1000)
                .upstreamIsochroneUnit(IsochroneUnit.METERS)
                .downstreamIsochrone(30)
                .downstreamIsochroneUnit(IsochroneUnit.SECONDS)
                .build());
        assertThat(singlePointMatch).isNotNull();
        assertThat(singlePointMatch.getId()).isEqualTo(ID);
        assertThat(singlePointMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        List<CandidateMatch> candidateMatches = getNearestCandidateMatches(singlePointMatch.getCandidateMatches());
        assertThat(candidateMatches).hasSize(1);
        assertThat(getSnappedPoints(candidateMatches)).hasSize(1);
        assertThat(singlePointMatch.getReliability()).isEqualTo(92.4716649970769);

        SinglePointMatch.CandidateMatch candidateMatch = singlePointMatch.getCandidateMatches().getFirst();
        assertThat(candidateMatch.getMatchedLinkId()).isEqualTo(3666958);

        // TODO upstream and downstream shouldn't contain matched segment itself?
        assertThat(candidateMatch.getUpstreamLinkIds()).containsExactlyInAnyOrder(3666958, 3666957, 3666956, 3666955, 3667003, 3667002,
                3667001, 3667000, 3666999, 3666998, 3666997, 3666996, 3666256, 3666973, 3666972, 3666971, 3666970, 3666969, 3666968,
                3666967, 3666966, 3666974, 3667137, 3667136, 3667135, 3667134, 3666244, 3666243, 3666242, 3666241, 3666240, 3666223,
                3667125, 3667124, 3667123, 3667122, 3667121, 3667120);
        assertThat(candidateMatch.getDownstreamLinkIds()).containsExactlyInAnyOrder(3666958, 3666098, 3666099, 3666100, 3666101, 3666102,
                3666103, 3666104, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217, 3666945, 3666946, 3666947, 3666948,
                3666949, 3666950, 3666951, 3666952, 3666943, 3666944, 3666953, 3666954, 3666123, 3666110, 3666111, 3666112, 3666113,
                3666114, 3666130, 3666115, 3666116, 3666117, 3666118, 3666119, 3666120);
    }

    private List<CandidateMatch> getNearestCandidateMatches(List<CandidateMatch> candidateMatches) {
        double cutoffDistance = candidateMatches.getFirst().getDistance() + DISTANCE_ROUNDING_ERROR;
        return candidateMatches.stream().filter(cm -> cm.getDistance() < cutoffDistance).toList();
    }

    private void assertThatUpstreamAndDownstreamAreNull(List<CandidateMatch> candidateMatches) {
        for (SinglePointMatch.CandidateMatch candidateMatch : candidateMatches) {
            assertThat(candidateMatch.getUpstream()).isNull();
            assertThat(candidateMatch.getDownstream()).isNull();
        }
    }

    private Set<Point> getSnappedPoints(List<CandidateMatch> candidateMatches) {
        return candidateMatches.stream()
                .map(SinglePointMatch.CandidateMatch::getSnappedPoint)
                .collect(Collectors.toSet());
    }
}
