package nu.ndw.nls.routingmapmatcher.viterbi;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

class ViterbiLineStringMapMatcherIT {

    private static final int END_FRACTION_1 = 1;
    private static final int START_FRACTION_0 = 0;
    
    private ViterbiLineStringMapMatcher viterbiLineStringMapMatcher;
    private final ObjectMapper mapper = OBJECT_MAPPER;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    @BeforeEach
    void setup() {
        viterbiLineStringMapMatcher = new ViterbiLineStringMapMatcher(TestNetworkProvider.getTestNetworkFromFile("/test-data/links.json"), CAR_FASTEST);
        geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
    }

    @SneakyThrows
    @Test
    void match_ok() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertSuccess(lineStringMatch, new Coordinate[]{new Coordinate(5.431641, 52.17898),
                new Coordinate(5.431601, 52.178947), new Coordinate(5.431138, 52.178641),
                new Coordinate(5.43111, 52.178622), new Coordinate(5.431077, 52.1786),
                new Coordinate(5.430663, 52.178413), new Coordinate(5.430569, 52.178379),
                new Coordinate(5.430526, 52.178363), new Coordinate(5.430206, 52.178246),
                new Coordinate(5.429884, 52.178165), new Coordinate(5.429882, 52.178165),
                new Coordinate(5.429557, 52.178112), new Coordinate(5.429507, 52.178104),
                new Coordinate(5.42918, 52.178064), new Coordinate(5.429103, 52.178055),
                new Coordinate(5.428902, 52.178033), new Coordinate(5.428641, 52.178005),
                new Coordinate(5.428467, 52.177998), new Coordinate(5.428057, 52.17798),
                new Coordinate(5.427836, 52.177971), new Coordinate(5.427747, 52.17797),
                new Coordinate(5.427079, 52.177963), new Coordinate(5.427025, 52.177964),
                new Coordinate(5.426949, 52.177966), new Coordinate(5.426695, 52.177971),
                new Coordinate(5.426323, 52.178013), new Coordinate(5.42631, 52.178015),
                new Coordinate(5.426132, 52.178046), new Coordinate(5.42593, 52.178082),
                new Coordinate(5.425628, 52.178159), new Coordinate(5.425557, 52.178177),
                new Coordinate(5.425238, 52.178299), new Coordinate(5.425227, 52.178303),
                new Coordinate(5.425037, 52.178409), new Coordinate(5.424792, 52.178546)});
    }

    @SneakyThrows
    @Test
    void match_ok_doubleEnd() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/test-data/matched_linestring_location_double_end.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(59.97209861861505);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(600767674).reversed(false).startFraction(0.6615449075921327)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(252408103).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(252408066).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600125366).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600126141).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600126144).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600126143).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600126037).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(600125593).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(250409010).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(0.30408706328439755).build());
        assertNull(lineStringMatch.getUpstreamLinkIds());
        assertNull(lineStringMatch.getDownstreamLinkIds());
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.6615449075921327);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.30408706328439755);
        assertThat(lineStringMatch.getWeight()).isEqualTo(2432.198);
        assertThat(lineStringMatch.getDuration()).isEqualTo(87.559);
        assertThat(lineStringMatch.getDistance()).isEqualTo(2432.198);
    }

    @SneakyThrows
    @Test
    void match_ok_simplify() {
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

        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
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
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.18611307333045);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(3666097).reversed(false).startFraction(0.8813982849151963)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666076).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666077).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666078).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666079).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666080).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666081).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666082).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666083).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666084).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666085).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1).build(),
                MatchedLink.builder().linkId(3666086).reversed(false).startFraction(START_FRACTION_0)
                        .endFraction(0.4580642228401559).build());
        assertThat(lineStringMatch.getUpstreamLinkIds())
                .containsExactlyInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061);
        assertThat(lineStringMatch.getDownstreamLinkIds())
                .containsExactlyInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217);
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.8813982849151963);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.4580642228401559);
        assertThat(lineStringMatch.getLineString()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(lineStringMatch.getWeight()).isEqualTo(519.767);
        assertThat(lineStringMatch.getDuration()).isEqualTo(18.713);
        assertThat(lineStringMatch.getDistance()).isEqualTo(519.767);
    }

    @SneakyThrows
    @Test
    void match_noMatch() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/unmatched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(15);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.NO_MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(0.0);
        assertThat(lineStringMatch.getLocationIndex()).isZero();
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
