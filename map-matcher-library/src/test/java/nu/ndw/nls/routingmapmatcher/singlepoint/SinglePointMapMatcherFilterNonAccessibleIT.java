package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;

@Slf4j
class SinglePointMapMatcherFilterNonAccessibleIT {

    private GeometryFactory geometryFactory;
    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    @BeforeEach
    void setup() {
        this.geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
        this.singlePointMapMatcher = new SinglePointMapMatcher(getTestNetwork(createLinks()), CAR_FASTEST);
    }

    /**
     * Test scenario contains one vertical line and two horizontal lines.
     * <p>
     * (0,2)    2 | (0,1) 0  ~ | 0-----2-----~-----6 1        2 (0,0)       (2,0)  (4,0)  (6,0) ~ pillar node
     *
     * @return
     */
    private List<TestLink> createLinks() {
        List<TestLink> links = new ArrayList<>();
        links.add(this.createLineLink(0, 0, 1, 0, 0, 0, 0, 1, 0, 2));
        links.add(this.createLineLink(1, 0, 2, 0, 0, 0, 2, 0));
        links.add(this.createLineLink(2, 2, 3, 100, 2, 0, 4, 0, 6, 0));
        return links;
    }

    private TestLink createLineLink(long id, long fromNodeId, long toNodeId, double speed, double... coordinates) {
        return TestLink.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speed)
                .reverseSpeedInKilometersPerHour(speed)
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
    void matchSinglePoint_ok_no_match() {
        SinglePointLocation singlePoint = this.createSinglePoint(0, 0, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);
        assertEquals(0, match.getCandidateMatches().size());

    }
    @Test
    void matchSinglePoint_ok_match() {
        SinglePointLocation singlePoint = this.createSinglePoint(1, 6, 0);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);
        assertEquals(2, match.getCandidateMatches().size());

    }


}
