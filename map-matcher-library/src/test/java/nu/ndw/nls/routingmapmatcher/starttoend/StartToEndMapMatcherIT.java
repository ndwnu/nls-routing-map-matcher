package nu.ndw.nls.routingmapmatcher.starttoend;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.model.linestring.ReliabilityCalculationType;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

class StartToEndMapMatcherIT {

    private StartToEndMapMatcher startToEndMapMatcher;
    private final ObjectMapper mapper = OBJECT_MAPPER;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    @BeforeEach
    void setup() {
        this.startToEndMapMatcher = new StartToEndMapMatcher(TestNetworkProvider.getTestNetworkFromFile("/test-data/links.json"), CAR_FASTEST);
        this.geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
    }

    @SneakyThrows
    @Test
    void match_ok_pointObservations() {
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{new Coordinate(5.431, 52.181),
                new Coordinate(5.423, 52.181)});
        LineStringLocation lineStringLocation = LineStringLocation.builder()
                .id(1)
                .locationIndex(1)
                .reversed(false)
                .lengthInMeters(543.0)
                .geometry(lineString)
                .reliabilityCalculationType(ReliabilityCalculationType.POINT_OBSERVATIONS)
                .build();
        LineStringMatch lineStringMatch = startToEndMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(1);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(73.88564657201005);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(1);
        assertThat(lineStringMatch.isReversed()).isFalse();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(6405237).reversed(false).build(),
                MatchedLink.builder().linkId(6405238).reversed(false).build(),
                MatchedLink.builder().linkId(6405239).reversed(false).build(),
                MatchedLink.builder().linkId(6405226).reversed(false).build(),
                MatchedLink.builder().linkId(6405227).reversed(false).build(),
                MatchedLink.builder().linkId(6405228).reversed(false).build(),
                MatchedLink.builder().linkId(6405229).reversed(false).build(),
                MatchedLink.builder().linkId(6405230).reversed(false).build(),
                MatchedLink.builder().linkId(6405231).reversed(false).build(),
                MatchedLink.builder().linkId(6405232).reversed(false).build(),
                MatchedLink.builder().linkId(6405233).reversed(false).build(),
                MatchedLink.builder().linkId(6369284).reversed(false).build(),
                MatchedLink.builder().linkId(6369285).reversed(false).build(),
                MatchedLink.builder().linkId(6369286).reversed(false).build(),
                MatchedLink.builder().linkId(6369287).reversed(false).build(),
                MatchedLink.builder().linkId(6369288).reversed(false).build());
        assertThat(lineStringMatch.getUpstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getDownstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.6496530239639768);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.9813310134286733);
        assertThat(lineStringMatch.getWeight()).isEqualTo(727.039);
        assertThat(lineStringMatch.getDuration()).isEqualTo(75.91);
        assertThat(lineStringMatch.getDistance()).isEqualTo(727.039);
    }

    @SneakyThrows
    @Test
    void match_ok_lineStringWithIsochrones() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = startToEndMapMatcher.match(lineStringLocation);
        assertSuccess(lineStringMatch, new Coordinate[]{new Coordinate(5.431641, 52.17898),
                new Coordinate(5.431601, 52.178947), new Coordinate(5.43111, 52.178622),
                new Coordinate(5.431077, 52.1786), new Coordinate(5.430663, 52.178413),
                new Coordinate(5.430526, 52.178363), new Coordinate(5.430206, 52.178246),
                new Coordinate(5.429884, 52.178165), new Coordinate(5.429882, 52.178165),
                new Coordinate(5.429507, 52.178104), new Coordinate(5.42918, 52.178064),
                new Coordinate(5.429103, 52.178055), new Coordinate(5.428641, 52.178005),
                new Coordinate(5.428467, 52.177998), new Coordinate(5.427836, 52.177971),
                new Coordinate(5.427747, 52.17797), new Coordinate(5.427079, 52.177963),
                new Coordinate(5.427025, 52.177964), new Coordinate(5.426695, 52.177971),
                new Coordinate(5.426323, 52.178013), new Coordinate(5.42631, 52.178015),
                new Coordinate(5.42593, 52.178082), new Coordinate(5.425628, 52.178159),
                new Coordinate(5.425557, 52.178177), new Coordinate(5.425227, 52.178303),
                new Coordinate(5.425037, 52.178409), new Coordinate(5.424792, 52.178546)});
    }

    @SneakyThrows
    @Test
    void match_ok_lineStringWithIsochrones_simplify() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation l = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringLocation lineStringLocation = LineStringLocation.builder()
                .id(l.getId())
                .upstreamIsochrone(l.getUpstreamIsochrone())
                .upstreamIsochroneUnit(l.getUpstreamIsochroneUnit())
                .downstreamIsochrone(l.getDownstreamIsochrone())
                .downstreamIsochroneUnit(l.getDownstreamIsochroneUnit())
                .locationIndex(l.getLocationIndex())
                .reversed(l.isReversed())
                .lengthInMeters(l.getLengthInMeters())
                .geometry(l.getGeometry())
                .reliabilityCalculationType(l.getReliabilityCalculationType())
                .radius(l.getRadius())
                .simplifyResponseGeometry(true)
                .build();

        LineStringMatch lineStringMatch = startToEndMapMatcher.match(lineStringLocation);
        assertSuccess(lineStringMatch, new Coordinate[]{new Coordinate(5.431641, 52.17898),
                new Coordinate(5.431077, 52.1786), new Coordinate(5.430663, 52.178413),
                new Coordinate(5.430206, 52.178246), new Coordinate(5.429884, 52.178165),
                new Coordinate(5.429507, 52.178104), new Coordinate(5.428641, 52.178005),
                new Coordinate(5.427836, 52.177971), new Coordinate(5.427079, 52.177963),
                new Coordinate(5.426695, 52.177971), new Coordinate(5.426323, 52.178013),
                new Coordinate(5.42593, 52.178082), new Coordinate(5.425557, 52.178177),
                new Coordinate(5.425227, 52.178303), new Coordinate(5.424792, 52.178546)});
    }

    private void assertSuccess(LineStringMatch lineStringMatch, Coordinate[] coordinates) {
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.29643981088304);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(3666097).reversed(false).build(),
                MatchedLink.builder().linkId(3666076).reversed(false).build(),
                MatchedLink.builder().linkId(3666077).reversed(false).build(),
                MatchedLink.builder().linkId(3666078).reversed(false).build(),
                MatchedLink.builder().linkId(3666079).reversed(false).build(),
                MatchedLink.builder().linkId(3666080).reversed(false).build(),
                MatchedLink.builder().linkId(3666081).reversed(false).build(),
                MatchedLink.builder().linkId(3666082).reversed(false).build(),
                MatchedLink.builder().linkId(3666083).reversed(false).build(),
                MatchedLink.builder().linkId(3666084).reversed(false).build(),
                MatchedLink.builder().linkId(3666085).reversed(false).build(),
                MatchedLink.builder().linkId(3666086).reversed(false).build());
        assertThat(lineStringMatch.getUpstreamLinkIds())
                .containsExactlyInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061);
        assertThat(lineStringMatch.getDownstreamLinkIds())
                .containsExactlyInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217);
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.8813982849151963);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.4580642228401559);
        assertThat(lineStringMatch.getLineString()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(lineStringMatch.getWeight()).isEqualTo(520.87);
        assertThat(lineStringMatch.getDuration()).isEqualTo(18.748);
        assertThat(lineStringMatch.getDistance()).isEqualTo(520.87);
    }

    @SneakyThrows
    @Test
    void match_noMatch() {
        LineString lineString = geometryFactory.createLineString(new Coordinate[]{new Coordinate(5.430, 52.180),
                new Coordinate(5.431, 52.181)});
        LineStringLocation lineStringLocation = LineStringLocation.builder()
                .id(1)
                .locationIndex(1)
                .reversed(false)
                .lengthInMeters(130.0)
                .geometry(lineString)
                .reliabilityCalculationType(ReliabilityCalculationType.POINT_OBSERVATIONS)
                .build();
        LineStringMatch lineStringMatch = startToEndMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(1);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.NO_MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(0.0);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(1);
        assertThat(lineStringMatch.isReversed()).isFalse();
        assertThat(lineStringMatch.getMatchedLinks()).isEmpty();
        assertThat(lineStringMatch.getUpstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getDownstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.0);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.0);
        assertThat(lineStringMatch.getWeight()).isEqualTo(0.0);
        assertThat(lineStringMatch.getDuration()).isEqualTo(0.0);
        assertThat(lineStringMatch.getDistance()).isEqualTo(0.0);
    }
}
