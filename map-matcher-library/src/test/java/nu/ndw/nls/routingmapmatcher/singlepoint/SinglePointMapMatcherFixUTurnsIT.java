
package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST_NO_U_TURNS;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.getTestNetwork;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchFilter;
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
class SinglePointMapMatcherFixUTurnsIT {

    @Autowired
    private SinglePointMapMatcherFactory singlePointMapMatcherFactory;

    @Autowired
    private GeometryFactoryWgs84 geometryFactoryWgs84;

    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    @BeforeEach
    void setup() {
        singlePointMapMatcher = singlePointMapMatcherFactory.createMapMatcher(getTestNetwork(createLinks()),
                CAR_FASTEST_NO_U_TURNS);
    }

    /**
     * Test scenario contains 4 horizontal segments.
     *
     * @return
     */
    private List<TestLink> createLinks() {
        // create 4 bi-directional links
        List<TestLink> links = new ArrayList<>();
        links.add(this.createLineLink(0, 10, 20, 50, 4.3773723, 52.0421300, 4.3775409, 52.0422341));
        links.add(this.createLineLink(1, 20, 30, 50, 4.3775409, 52.0422341, 4.3777344, 52.0423673));
        links.add(this.createLineLink(2, 30, 40, 50, 4.3777344, 52.0423673, 4.3779800, 52.0425067));
        links.add(this.createLineLink(3, 40, 50, 50, 4.3779800, 52.0425067, 4.378165, 52.0426357));
        return links;
    }

    private TestLink createLineLink(long id, long fromNodeId, long toNodeId, double speed, double... coordinates) {
        return TestLink.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speed)
                .reverseSpeedInKilometersPerHour(speed)
                .distanceInMeters(100)
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
        WKTReader wktReader = new WKTReader(geometryFactoryWgs84);
        return (LineString) wktReader.read(lineStringSb.toString());
    }

    private SinglePointLocation createSinglePoint(int id, double x, double y) {
        Point point = geometryFactoryWgs84.createPoint(new Coordinate(x, y));
        return SinglePointLocation.builder()
                .id(id)
                .point(point)
                .bearingFilter(new BearingFilter(41, 45))
                .upstreamIsochrone(5000)
                .matchFilter(MatchFilter.FIRST)
                .upstreamIsochroneUnit(IsochroneUnit.METERS)
                .build();
    }


    @Test
    void matchSinglePoint_ok_should_return_isochrone_without_u_turns() {
        SinglePointLocation singlePoint = this.createSinglePoint(1, 4.3781143, 52.0425856);
        SinglePointMatch match = this.singlePointMapMatcher.match(singlePoint);
        assertEquals(1, match.getCandidateMatches().size());
        CandidateMatch candidate = match.getCandidateMatches().getFirst();
        //should have 4 sections not 8
        assertThat(candidate.getUpstream()).hasSize(4);

    }


}
