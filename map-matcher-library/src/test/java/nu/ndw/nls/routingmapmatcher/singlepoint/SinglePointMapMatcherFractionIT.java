package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestNetwork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class SinglePointMapMatcherFractionIT {

    @Autowired
    private SinglePointMapMatcherFactory singlePointMapMatcherFactory;
    @Autowired
    private GeometryFactoryWgs84 geometryFactory;
    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    @BeforeEach
    void setup() {
        singlePointMapMatcher = singlePointMapMatcherFactory.createMapMatcher(getTestNetwork(createLinks()),
                CAR_FASTEST);
    }

    /**
     * Test scenario contains one vertical line and two horizontal lines. With and without pilar nodes.
     * <p>
     * (0,2)    2 | (0,1) 0  ~ | 0-----2-----~-----6 1        2 (0,0)       (2,0)  (4,0)  (6,0) ~ pillar node
     *
     * @return
     */
    private List<TestLink> createLinks() {
        List<TestLink> links = new ArrayList<>();
        links.add(this.createLineLink(0, 0, 1, 0, 0, 0, 1, 0, 2));
        links.add(this.createLineLink(1, 0, 2, 0, 0, 2, 0));
        links.add(this.createLineLink(2, 2, 3, 2, 0, 4, 0, 6, 0));

        return links;
    }

    private TestLink createLineLink(long id, long fromNodeId, long toNodeId, double... coordinates) {
        return TestLink.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(100)
                .reverseSpeedInKilometersPerHour(0)
                .distanceInMeters(1000)
                .geometry(createLineStringWktReader(coordinates))
                .build();
    }

    @SneakyThrows
    private LineString createLineStringWktReader(double... coordinates) {
        if (coordinates == null || coordinates.length % 2 != 0) {
            throw new IllegalStateException("Must have coordinates and must come in pairs of two (x, y)");
        }

        StringBuilder lineStringSb = new StringBuilder("LINESTRING(");
        for (int i = 0; i < coordinates.length; i += 2) {
            if (i > 0) {
                lineStringSb.append(", ");
            }
            lineStringSb.append(coordinates[i]);
            lineStringSb.append(" ");
            lineStringSb.append(coordinates[i + 1]);
        }

        lineStringSb.append(")");

        log.debug("Loading line string: {}", lineStringSb);
        WKTReader wktReader = new WKTReader(this.geometryFactory);
        return (LineString) wktReader.read(lineStringSb.toString());
    }

    private SinglePointLocation createSinglePoint(int id, double x, double y) {
        Point point = this.geometryFactory.createPoint(new Coordinate(x, y));
        return SinglePointLocation.builder()
                .id(id)
                .point(point)
                .build();
    }

    @Test
    void matchSinglePoint_fraction_vertical_towerBaseNode() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 0, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        assertEquals(2, match.getCandidateMatches().size());

        Optional<CandidateMatch> first = match.getCandidateMatches().stream()
                .filter(c -> c.getMatchedLinkId() == 0).findFirst();
        assertTrue(first.isPresent());
        assertEquals(0, first.get().getFraction(), 0.001);

        Optional<CandidateMatch> second = match.getCandidateMatches().stream()
                .filter(c -> c.getMatchedLinkId() == 1).findFirst();
        assertTrue(second.isPresent());
        assertEquals(0, second.get().getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_vertical_edge() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 0, 1);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(0, candidateMatch.getMatchedLinkId());
        assertEquals(0.5, candidateMatch.getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_vertical_towerAdjacentNode() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 0, 2);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(0, candidateMatch.getMatchedLinkId());
        assertEquals(1, candidateMatch.getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_towerBaseNode_returns_two_candidates_with_different_bearings() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 0, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);
        assertThat(match.getCandidateMatches()).hasSize(2);
        CandidateMatch candidateMatch1 = match.getCandidateMatches().get(0);
        assertEquals(0, candidateMatch1.getMatchedLinkId());
        assertEquals(0, candidateMatch1.getBearing());
        assertEquals(0, candidateMatch1.getFraction(), 0.001);
        CandidateMatch candidateMatch2 = match.getCandidateMatches().get(1);
        assertEquals(1, candidateMatch2.getMatchedLinkId());
        assertEquals(90, candidateMatch2.getBearing());
        assertEquals(0, candidateMatch2.getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_towerAdjacentNode() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 6, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(2, candidateMatch.getMatchedLinkId());
        assertEquals(1, candidateMatch.getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_pillarHalfWay() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 4, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(2, candidateMatch.getMatchedLinkId());
        assertEquals(0.5, candidateMatch.getFraction(), 0.001);
    }

    @Test
    void matchSinglePoint_fraction_edgeQuarterWay() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 3, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(2, candidateMatch.getMatchedLinkId());
        assertEquals(0.25, candidateMatch.getFraction(), 0.001);
    }
}
