package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.WKTReader;

@Slf4j
class SinglePointMapMatcherFractionTest {

    private GeometryFactory geometryFactory;
    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    @BeforeEach
    private void setup() {
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);

        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test_network")
                .linkSupplier(this.createLinks()::iterator)
                .build();

        GraphHopperSinglePointMapMatcherFactory graphHopperSinglePointMapMatcherFactory =
                new GraphHopperSinglePointMapMatcherFactory(new NetworkGraphHopperFactory());

        this.singlePointMapMatcher = graphHopperSinglePointMapMatcherFactory.createMapMatcher(routingNetwork);
    }

    /**
     * Test scenario contains one vertical line and two horizontal lines. With and without pilar nodes.
     *
     * (0,2)    2
     *          |
     * (0,1) 0  ~
     *          |
     *          0-----2-----~-----6
     *             1        2
     * (0,0)       (2,0)  (4,0)  (6,0)
     * ~ pillar node
     *
     * @return
     */
    private List<Link> createLinks() {
        List<Link> links = new ArrayList<>();
        links.add(this.createLineLink(0, 0, 1, 0, 0, 0, 1, 0, 2));
        links.add(this.createLineLink(1, 0, 2, 0, 0, 2, 0));
        links.add(this.createLineLink(2, 2, 3, 2, 0, 4, 0, 6, 0));

        return links;
    }

    private Link createLineLink(long id, long fromNodeId, long toNodeId, double... coordinates) {
        return Link.builder()
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
    void matchSinglePoint_fraction_towerBaseNode() {
        SinglePointLocation singlePoint = this.createSinglePoint(123, 0, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);

        CandidateMatch candidateMatch = match.getCandidateMatches().get(0);
        assertEquals(1, candidateMatch.getMatchedLinkId());
        assertEquals(0, candidateMatch.getFraction(), 0.001);
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
