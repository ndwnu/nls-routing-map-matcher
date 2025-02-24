package nu.ndw.nls.routingmapmatcher.viterbi;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestLineStringProvider;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.CoordinateHelper;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestConfig.class})
class ViterbiLineStringMapMatcherIT {

    private static final int END_FRACTION_1 = 1;
    private static final int START_FRACTION_0 = 0;
    private static final String ROUND_TRIP_COORDINATES =
            "5.426229655945605,52.180997682499225;5.426016932174917,52.18049042990458;"
            + "5.425796329747783,52.17989138129437;5.425890873644789,52.179760942221066;"
            + "5.427159337606241,52.17978992871468;5.429129002141082,52.179852732719894;"
            + "5.430287164887744,52.17991070556937;5.430618068529668,52.180016988929594;"
            + "5.4305786752388485,52.18037931665884;5.429491420414905,52.180538739924884;"
            + "5.426552680928609,52.18097352774487;5.426221777286685,52.18102183724045;"
            + "5.426040568150427,52.18064985277195;5.425819965722042,52.18009428575948;"
            + "5.425764815114661,52.1798044219548;5.426489651663502,52.179722293533246;"
            + "5.429160516773038,52.1798044219548;5.430562917922288,52.17995418515633;"
            + "5.430618068529668,52.18030202032435;5.430184742331875,52.180509753918926;"
            + "5.428774462524984,52.18068366967026;5.426276927894065,52.18106048479896";
    @Autowired
    private ViterbiLinestringMapMatcherFactory viterbiLinestringMapMatcherFactory;

    @Autowired
    private GeometryFactoryWgs84 geometryFactory;

    private ViterbiLineStringMapMatcher viterbiLineStringMapMatcher;

    @SneakyThrows
    @BeforeEach
    void setup() {
        viterbiLineStringMapMatcher = viterbiLinestringMapMatcherFactory.createMapMatcher(
                TestNetworkProvider.getTestNetworkFromFile("/test-data/network.geojson"), CAR);
    }

    @SneakyThrows
    @Test
    void match_ok() {
        LineStringLocation lineStringLocation = TestLineStringProvider.getLineStringLocation(
                "/test-data/matched_linestring_location.geojson");
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        verifySumDistanceOfIndividualRoadSections(lineStringMatch);
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
        LineStringLocation lineStringLocation = TestLineStringProvider.getLineStringLocation(
                "/test-data/matched_linestring_location_double_end.geojson");
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        verifySumDistanceOfIndividualRoadSections(lineStringMatch);
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(97.50886628818601);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).hasSize(10);

        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder()
                        .linkId(600767674)
                        .reversed(false)
                        .distance(55.714350053181654)
                        .startFraction(0.6615449075921327)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(252408103)
                        .reversed(false)
                        .distance(161.02326851252982)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(252408066)
                        .reversed(false)
                        .distance(15.570829978849499)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600125366)
                        .reversed(false)
                        .distance(629.530359881841)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600126141)
                        .reversed(false)
                        .distance(508.76706750198866)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600126144)
                        .reversed(false)
                        .distance(9.944331278004947)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600126143)
                        .reversed(false)
                        .distance(33.293558051510196)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600126037)
                        .reversed(false)
                        .distance(413.21490814764564)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(600125593)
                        .reversed(false)
                        .distance(116.64863705130153)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(250409010)
                        .reversed(false)
                        .distance(491.61806977151326)
                        .startFraction(START_FRACTION_0)
                        .endFraction(0.30408706328439755)
                        .build());
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
        LineStringLocation l = TestLineStringProvider.getLineStringLocation("/test-data/matched_linestring_location.geojson");
        LineStringLocation lineStringLocation = LineStringLocation.builder()
                .id(l.getId())
                .upstreamIsochrone(l.getUpstreamIsochrone())
                .upstreamIsochroneUnit(l.getUpstreamIsochroneUnit())
                .downstreamIsochrone(l.getDownstreamIsochrone())
                .downstreamIsochroneUnit(l.getDownstreamIsochroneUnit())
                .locationIndex(l.getLocationIndex())
                .reversed(l.isReversed())
                .geometry(l.getGeometry())
                .reliabilityCalculationType(l.getReliabilityCalculationType())
                .radius(l.getRadius())
                .simplifyResponseGeometry(true)
                .build();

        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        verifySumDistanceOfIndividualRoadSections(lineStringMatch);
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
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.75305757443932);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder()
                        .linkId(3666097)
                        .reversed(false)
                        .distance(4.606277878496104)
                        .startFraction(0.8813982849151963)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666076)
                        .reversed(false)
                        .distance(49.35391266663205)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666077)
                        .reversed(false)
                        .distance(49.370209885281966)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666078)
                        .reversed(false)
                        .distance(49.4042427750329)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666079)
                        .reversed(false)
                        .distance(49.34170254306041)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666080)
                        .reversed(false)
                        .distance(49.37871322376412)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666081)
                        .reversed(false)
                        .distance(49.3572706954873)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666082)
                        .reversed(false)
                        .distance(49.39696866946102)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666083)
                        .reversed(false)
                        .distance(49.37634675570945)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666084)
                        .reversed(false)
                        .distance(49.406527048706316)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666085)
                        .reversed(false)
                        .distance(49.37832995331975)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .build(),
                MatchedLink.builder()
                        .linkId(3666086)
                        .reversed(false)
                        .distance(22.597894098636313)
                        .startFraction(START_FRACTION_0)
                        .endFraction(0.4580642228401559)
                        .build());
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
        LineStringLocation lineStringLocation = TestLineStringProvider.getLineStringLocation(
                "/test-data/unmatched_linestring_location.geojson");
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

    @SneakyThrows
    @Test
    void match_ok_distanceRoundTrip() {
        // coordinates are a circular route with part of the road-sections traversed twice in the same direction
        LineString lineString = geometryFactory.createLineString(
                CoordinateHelper
                        .getCoordinatesFromString(ROUND_TRIP_COORDINATES)
                        .toArray(new Coordinate[]{}));
        LineStringLocation lineStringLocation = LineStringLocation
                .builder()
                .id(1)
                .locationIndex(1)
                .reversed(false)
                .geometry(lineString)
                .build();
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        verifySumDistanceOfIndividualRoadSections(lineStringMatch);
        // start point will be traversed twice
        assertThat(lineStringMatch.getMatchedLinks())
                .filteredOn("linkId", 6405183)
                .hasSize(2);
        // end point will be traversed twice
        assertThat(lineStringMatch.getMatchedLinks())
                .filteredOn("linkId", 6405177)
                .hasSize(2);
    }

    private static void verifySumDistanceOfIndividualRoadSections(LineStringMatch lineStringMatch) {
        assertThat((Double) lineStringMatch.getMatchedLinks()
                .stream()
                .map(MatchedLink::getDistance)
                .mapToDouble(Double::doubleValue)
                .sum())
                .isCloseTo(lineStringMatch.getDistance(), Percentage.withPercentage(0.3));
    }
}
