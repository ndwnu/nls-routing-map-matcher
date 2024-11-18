package nu.ndw.nls.routingmapmatcher.viterbi;

import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.CAR;
import static nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.OBJECT_MAPPER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.TestConfig;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider;
import nu.ndw.nls.routingmapmatcher.util.CoordinateHelper;
import org.apache.commons.io.IOUtils;
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
    private final ObjectMapper mapper = OBJECT_MAPPER;


    @SneakyThrows
    @BeforeEach
    void setup() {
        viterbiLineStringMapMatcher = viterbiLinestringMapMatcherFactory.createMapMatcher(
                TestNetworkProvider.getTestNetworkFromFile("/test-data/links.json"), CAR);
    }

    @SneakyThrows
    @Test
    void match_ok() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
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
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/test-data/matched_linestring_location_double_end.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        verifySumDistanceOfIndividualRoadSections(lineStringMatch);
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(59.97209861861505);
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
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.964446976417325, 52.52493178311345), new Coordinate(4.9646097, 52.5251947),
                                new Coordinate(4.9647338, 52.5254009)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9638460, 52.5240273), new Coordinate(4.9638486, 52.5240300),
                                new Coordinate(4.9638807, 52.5240766), new Coordinate(4.9641529, 52.5245213),
                                new Coordinate(4.9641804, 52.5245538), new Coordinate(4.9642282, 52.5245922),
                                new Coordinate(4.9642472, 52.5246105), new Coordinate(4.9642663, 52.5246340),
                                new Coordinate(4.9642931, 52.5246728), new Coordinate(4.9643302, 52.5247323),
                                new Coordinate(4.9644075, 52.5248680), new Coordinate(4.9646097, 52.5251947),
                                new Coordinate(4.9647338, 52.5254009)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(252408103)
                        .reversed(false)
                        .distance(161.02326851252982)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9647338, 52.5254009), new Coordinate(4.9648070, 52.5255225),
                                new Coordinate(4.9648922, 52.5256561), new Coordinate(4.9649482, 52.5257503),
                                new Coordinate(4.9650334, 52.5258770), new Coordinate(4.9650454, 52.5258888),
                                new Coordinate(4.9650570, 52.5258972), new Coordinate(4.9650983, 52.5259192),
                                new Coordinate(4.9651475, 52.5259832), new Coordinate(4.9655707, 52.5266754),
                                new Coordinate(4.9656213, 52.5267374)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9647338, 52.5254009), new Coordinate(4.9648070, 52.5255225),
                                new Coordinate(4.9648922, 52.5256561), new Coordinate(4.9649482, 52.5257503),
                                new Coordinate(4.9650334, 52.5258770), new Coordinate(4.9650454, 52.5258888),
                                new Coordinate(4.9650570, 52.5258972), new Coordinate(4.9650983, 52.5259192),
                                new Coordinate(4.9651475, 52.5259832), new Coordinate(4.9655707, 52.5266754),
                                new Coordinate(4.9656213, 52.5267374)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(252408066)
                        .reversed(false)
                        .distance(15.570829978849499)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9656213, 52.5267374), new Coordinate(4.9656259, 52.5268773)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9656213, 52.5267374), new Coordinate(4.9656259, 52.5268773)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600125366)
                        .reversed(false)
                        .distance(629.530359881841)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9656259, 52.5268773), new Coordinate(4.9652786, 52.5269516),
                                new Coordinate(4.9652110, 52.5269697), new Coordinate(4.9651420, 52.5269853),
                                new Coordinate(4.9650730, 52.5270008), new Coordinate(4.9650038, 52.5270162),
                                new Coordinate(4.9649346, 52.5270315), new Coordinate(4.9648655, 52.5270469),
                                new Coordinate(4.9647963, 52.5270623), new Coordinate(4.9647272, 52.5270778),
                                new Coordinate(4.9646582, 52.5270933), new Coordinate(4.9645893, 52.5271092),
                                new Coordinate(4.9645204, 52.5271251), new Coordinate(4.9644516, 52.5271409),
                                new Coordinate(4.9643490, 52.5271645), new Coordinate(4.9643138, 52.5271728),
                                new Coordinate(4.9642452, 52.527189), new Coordinate(4.9641765, 52.5272052),
                                new Coordinate(4.9641079, 52.5272215), new Coordinate(4.9640394, 52.5272379),
                                new Coordinate(4.9639709, 52.5272543), new Coordinate(4.9639024, 52.5272707),
                                new Coordinate(4.9638340, 52.5272872), new Coordinate(4.9637656, 52.5273038),
                                new Coordinate(4.9636972, 52.5273205), new Coordinate(4.9636288, 52.5273371),
                                new Coordinate(4.9635605, 52.5273538), new Coordinate(4.9634922, 52.5273705),
                                new Coordinate(4.9634239, 52.5273873), new Coordinate(4.9633558, 52.5274043),
                                new Coordinate(4.9632877, 52.5274214), new Coordinate(4.9632196, 52.5274385),
                                new Coordinate(4.9631516, 52.5274556), new Coordinate(4.9630835, 52.5274727),
                                new Coordinate(4.9629617, 52.5275033), new Coordinate(4.9629474, 52.5275069),
                                new Coordinate(4.9628791, 52.5275237), new Coordinate(4.9628108, 52.5275404),
                                new Coordinate(4.9627424, 52.5275569), new Coordinate(4.9626739, 52.5275734),
                                new Coordinate(4.9626054, 52.5275899), new Coordinate(4.9625369, 52.5276064),
                                new Coordinate(4.9624684, 52.5276227), new Coordinate(4.9623999, 52.5276391),
                                new Coordinate(4.9623313, 52.5276555), new Coordinate(4.9622627, 52.5276717),
                                new Coordinate(4.9621940, 52.5276878), new Coordinate(4.9621160, 52.5277061),
                                new Coordinate(4.9620564, 52.5277198), new Coordinate(4.9619873, 52.5277352),
                                new Coordinate(4.9619181, 52.5277506), new Coordinate(4.9618490, 52.527766),
                                new Coordinate(4.9617798, 52.5277814), new Coordinate(4.9617107, 52.5277968),
                                new Coordinate(4.9616416, 52.5278122), new Coordinate(4.9615724, 52.5278275),
                                new Coordinate(4.9615032, 52.5278429), new Coordinate(4.9614340, 52.5278582),
                                new Coordinate(4.9613648, 52.5278735), new Coordinate(4.9612957, 52.5278888),
                                new Coordinate(4.9612265, 52.5279041), new Coordinate(4.9612059728764395, 52.527908668174895)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9656259, 52.5268773), new Coordinate(4.9652786, 52.5269516),
                                new Coordinate(4.9652110, 52.5269697), new Coordinate(4.9651420, 52.5269853),
                                new Coordinate(4.9650730, 52.5270008), new Coordinate(4.9650038, 52.5270162),
                                new Coordinate(4.9649346, 52.5270315), new Coordinate(4.9648655, 52.5270469),
                                new Coordinate(4.9647963, 52.5270623), new Coordinate(4.9647272, 52.5270778),
                                new Coordinate(4.9646582, 52.5270933), new Coordinate(4.9645893, 52.5271092),
                                new Coordinate(4.9645204, 52.5271251), new Coordinate(4.9644516, 52.5271409),
                                new Coordinate(4.9643490, 52.5271645), new Coordinate(4.9643138, 52.5271728),
                                new Coordinate(4.9642452, 52.527189), new Coordinate(4.9641765, 52.5272052),
                                new Coordinate(4.9641079, 52.5272215), new Coordinate(4.9640394, 52.5272379),
                                new Coordinate(4.9639709, 52.5272543), new Coordinate(4.9639024, 52.5272707),
                                new Coordinate(4.9638340, 52.5272872), new Coordinate(4.9637656, 52.5273038),
                                new Coordinate(4.9636972, 52.5273205), new Coordinate(4.9636288, 52.5273371),
                                new Coordinate(4.9635605, 52.5273538), new Coordinate(4.9634922, 52.5273705),
                                new Coordinate(4.9634239, 52.5273873), new Coordinate(4.9633558, 52.5274043),
                                new Coordinate(4.9632877, 52.5274214), new Coordinate(4.9632196, 52.5274385),
                                new Coordinate(4.9631516, 52.5274556), new Coordinate(4.9630835, 52.5274727),
                                new Coordinate(4.9629617, 52.5275033), new Coordinate(4.9629474, 52.5275069),
                                new Coordinate(4.9628791, 52.5275237), new Coordinate(4.9628108, 52.5275404),
                                new Coordinate(4.9627424, 52.5275569), new Coordinate(4.9626739, 52.5275734),
                                new Coordinate(4.9626054, 52.5275899), new Coordinate(4.9625369, 52.5276064),
                                new Coordinate(4.9624684, 52.5276227), new Coordinate(4.9623999, 52.5276391),
                                new Coordinate(4.9623313, 52.5276555), new Coordinate(4.9622627, 52.5276717),
                                new Coordinate(4.9621940, 52.5276878), new Coordinate(4.9621160, 52.5277061),
                                new Coordinate(4.9620564, 52.5277198), new Coordinate(4.9619873, 52.5277352),
                                new Coordinate(4.9619181, 52.5277506), new Coordinate(4.9618490, 52.527766),
                                new Coordinate(4.9617798, 52.5277814), new Coordinate(4.9617107, 52.5277968),
                                new Coordinate(4.9616416, 52.5278122), new Coordinate(4.9615724, 52.5278275),
                                new Coordinate(4.9615032, 52.5278429), new Coordinate(4.9614340, 52.5278582),
                                new Coordinate(4.9613648, 52.5278735), new Coordinate(4.9612957, 52.5278888),
                                new Coordinate(4.9612265, 52.5279041), new Coordinate(4.9611573, 52.5279195),
                                new Coordinate(4.9610881, 52.5279348), new Coordinate(4.9610189, 52.5279502),
                                new Coordinate(4.9609498, 52.5279655), new Coordinate(4.9608806, 52.5279809),
                                new Coordinate(4.9608114, 52.5279962), new Coordinate(4.9607423, 52.5280116),
                                new Coordinate(4.9606731, 52.5280269), new Coordinate(4.9606039, 52.5280423),
                                new Coordinate(4.9605348, 52.5280577), new Coordinate(4.9604656, 52.528073),
                                new Coordinate(4.9603965, 52.5280884), new Coordinate(4.9603273, 52.5281037),
                                new Coordinate(4.9602581, 52.528119), new Coordinate(4.9601889, 52.5281344),
                                new Coordinate(4.9601198, 52.5281498), new Coordinate(4.9600506, 52.5281651),
                                new Coordinate(4.9599814, 52.5281805), new Coordinate(4.9599123, 52.528196),
                                new Coordinate(4.9598433, 52.5282115), new Coordinate(4.9597742, 52.5282271),
                                new Coordinate(4.9597052, 52.5282427), new Coordinate(4.9596361, 52.5282582),
                                new Coordinate(4.9595671, 52.5282738), new Coordinate(4.9594980, 52.5282893),
                                new Coordinate(4.9594290, 52.5283049), new Coordinate(4.9593599, 52.5283204),
                                new Coordinate(4.9592909, 52.528336), new Coordinate(4.9592652, 52.5283418),
                                new Coordinate(4.9592220, 52.5283515), new Coordinate(4.9591529, 52.5283671),
                                new Coordinate(4.9590839, 52.5283826), new Coordinate(4.9590148, 52.5283982),
                                new Coordinate(4.9589457, 52.5284137), new Coordinate(4.9588767, 52.5284292),
                                new Coordinate(4.9588076, 52.5284448), new Coordinate(4.9587385, 52.5284602),
                                new Coordinate(4.9586694, 52.5284756), new Coordinate(4.9586002, 52.528491),
                                new Coordinate(4.9585311, 52.5285064), new Coordinate(4.9584619, 52.5285218),
                                new Coordinate(4.9583928, 52.5285372), new Coordinate(4.9583236, 52.5285526),
                                new Coordinate(4.9582545, 52.528568), new Coordinate(4.9581853, 52.5285834),
                                new Coordinate(4.9581162, 52.5285988), new Coordinate(4.9580470, 52.5286142),
                                new Coordinate(4.9579779, 52.5286296), new Coordinate(4.9579088, 52.528645),
                                new Coordinate(4.9578396, 52.5286604), new Coordinate(4.9577705, 52.5286758),
                                new Coordinate(4.9577013, 52.5286912), new Coordinate(4.9574341, 52.528768),
                                new Coordinate(4.9573650, 52.5287835), new Coordinate(4.9572959, 52.5287989),
                                new Coordinate(4.9572267, 52.5288143), new Coordinate(4.9571576, 52.5288297),
                                new Coordinate(4.9570884, 52.5288451), new Coordinate(4.9569502, 52.528876)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600126141)
                        .reversed(false)
                        .distance(508.76706750198866)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9569502, 52.528876), new Coordinate(4.9568121, 52.5289068),
                                new Coordinate(4.9567431, 52.5289222), new Coordinate(4.9566741, 52.5289375),
                                new Coordinate(4.9566052, 52.5289529), new Coordinate(4.9565362, 52.5289683),
                                new Coordinate(4.9563982, 52.5289991), new Coordinate(4.9561818, 52.5290295),
                                new Coordinate(4.9561129, 52.5290452), new Coordinate(4.9560438, 52.5290603),
                                new Coordinate(4.9559748, 52.5290757), new Coordinate(4.9559059, 52.5290912),
                                new Coordinate(4.9558370, 52.5291066), new Coordinate(4.9557680, 52.5291221),
                                new Coordinate(4.9556991, 52.5291375), new Coordinate(4.95563, 52.5291528),
                                new Coordinate(4.9555610, 52.5291682), new Coordinate(4.9554920, 52.5291835),
                                new Coordinate(4.9554229, 52.5291988), new Coordinate(4.9553539, 52.5292141),
                                new Coordinate(4.9552849, 52.5292293), new Coordinate(4.9552158, 52.5292446),
                                new Coordinate(4.9551468, 52.5292599), new Coordinate(4.9550777, 52.5292752),
                                new Coordinate(4.9550087, 52.5292904), new Coordinate(4.9549396, 52.5293057),
                                new Coordinate(4.9548706, 52.529321), new Coordinate(4.9548015, 52.5293363),
                                new Coordinate(4.9547325, 52.5293516), new Coordinate(4.9546634, 52.5293669),
                                new Coordinate(4.9545945, 52.5293823), new Coordinate(4.9545255, 52.5293976),
                                new Coordinate(4.9544565, 52.529413), new Coordinate(4.9543875, 52.5294284),
                                new Coordinate(4.9543185, 52.5294438), new Coordinate(4.9542495, 52.5294592),
                                new Coordinate(4.9541805, 52.5294745), new Coordinate(4.9541115, 52.5294899),
                                new Coordinate(4.9540425, 52.5295053), new Coordinate(4.9539735, 52.5295206),
                                new Coordinate(4.9539052, 52.5295371), new Coordinate(4.9512753, 52.5301625),
                                new Coordinate(4.9512358, 52.5301725), new Coordinate(4.9511694, 52.5301917),
                                new Coordinate(4.9511033, 52.5302114), new Coordinate(4.9510372, 52.5302311),
                                new Coordinate(4.9509706, 52.5302501), new Coordinate(4.9509040, 52.530269),
                                new Coordinate(4.9508369, 52.5302873), new Coordinate(4.9507696, 52.5303051),
                                new Coordinate(4.9507364, 52.5303139), new Coordinate(4.9507019, 52.530323),
                                new Coordinate(4.9506346, 52.5303409), new Coordinate(4.9505672, 52.5303588),
                                new Coordinate(4.9504999, 52.5303767), new Coordinate(4.9504325, 52.5303945),
                                new Coordinate(4.9503654, 52.5304128), new Coordinate(4.9502971, 52.5304293),
                                new Coordinate(4.9499726, 52.5305357)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9569502, 52.528876), new Coordinate(4.9568121, 52.5289068),
                                new Coordinate(4.9567431, 52.5289222), new Coordinate(4.9566741, 52.5289375),
                                new Coordinate(4.9566052, 52.5289529), new Coordinate(4.9565362, 52.5289683),
                                new Coordinate(4.9563982, 52.5289991), new Coordinate(4.9561818, 52.5290295),
                                new Coordinate(4.9561129, 52.5290452), new Coordinate(4.9560438, 52.5290603),
                                new Coordinate(4.9559748, 52.5290757), new Coordinate(4.9559059, 52.5290912),
                                new Coordinate(4.9558370, 52.5291066), new Coordinate(4.9557680, 52.5291221),
                                new Coordinate(4.9556991, 52.5291375), new Coordinate(4.95563, 52.5291528),
                                new Coordinate(4.9555610, 52.5291682), new Coordinate(4.9554920, 52.5291835),
                                new Coordinate(4.9554229, 52.5291988), new Coordinate(4.9553539, 52.5292141),
                                new Coordinate(4.9552849, 52.5292293), new Coordinate(4.9552158, 52.5292446),
                                new Coordinate(4.9551468, 52.5292599), new Coordinate(4.9550777, 52.5292752),
                                new Coordinate(4.9550087, 52.5292904), new Coordinate(4.9549396, 52.5293057),
                                new Coordinate(4.9548706, 52.529321), new Coordinate(4.9548015, 52.5293363),
                                new Coordinate(4.9547325, 52.5293516), new Coordinate(4.9546634, 52.5293669),
                                new Coordinate(4.9545945, 52.5293823), new Coordinate(4.9545255, 52.5293976),
                                new Coordinate(4.9544565, 52.529413), new Coordinate(4.9543875, 52.5294284),
                                new Coordinate(4.9543185, 52.5294438), new Coordinate(4.9542495, 52.5294592),
                                new Coordinate(4.9541805, 52.5294745), new Coordinate(4.9541115, 52.5294899),
                                new Coordinate(4.9540425, 52.5295053), new Coordinate(4.9539735, 52.5295206),
                                new Coordinate(4.9539052, 52.5295371), new Coordinate(4.9512753, 52.5301625),
                                new Coordinate(4.9512358, 52.5301725), new Coordinate(4.9511694, 52.5301917),
                                new Coordinate(4.9511033, 52.5302114), new Coordinate(4.9510372, 52.5302311),
                                new Coordinate(4.9509706, 52.5302501), new Coordinate(4.9509040, 52.530269),
                                new Coordinate(4.9508369, 52.5302873), new Coordinate(4.9507696, 52.5303051),
                                new Coordinate(4.9507364, 52.5303139), new Coordinate(4.9507019, 52.530323),
                                new Coordinate(4.9506346, 52.5303409), new Coordinate(4.9505672, 52.5303588),
                                new Coordinate(4.9504999, 52.5303767), new Coordinate(4.9504325, 52.5303945),
                                new Coordinate(4.9503654, 52.5304128), new Coordinate(4.9502971, 52.5304293),
                                new Coordinate(4.9499726, 52.5305357)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600126144)
                        .reversed(false)
                        .distance(9.944331278004947)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9499726, 52.5305357), new Coordinate(4.9499091, 52.5305527),
                                new Coordinate(4.9498387, 52.5305720)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9499726, 52.5305357), new Coordinate(4.9499091, 52.5305527),
                                new Coordinate(4.9498387, 52.5305720)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600126143)
                        .reversed(false)
                        .distance(33.293558051510196)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9498387, 52.5305720), new Coordinate(4.9496557, 52.5302944)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9498387, 52.5305720), new Coordinate(4.9496557, 52.5302944)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600126037)
                        .reversed(false)
                        .distance(413.21490814764564)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9496557, 52.5302944), new Coordinate(4.9493614, 52.5298027),
                                new Coordinate(4.9491665, 52.5295238), new Coordinate(4.9489671, 52.5291903),
                                new Coordinate(4.9489072, 52.5290998), new Coordinate(4.9488410, 52.5290184),
                                new Coordinate(4.9487860, 52.5289628), new Coordinate(4.9487467, 52.5289261),
                                new Coordinate(4.9487012, 52.5288903), new Coordinate(4.9485991, 52.5288225),
                                new Coordinate(4.9485460, 52.5287913), new Coordinate(4.9484652, 52.5287496),
                                new Coordinate(4.9484056, 52.5287231), new Coordinate(4.9483431, 52.5286992),
                                new Coordinate(4.9482489, 52.5286692), new Coordinate(4.9481818, 52.5286513),
                                new Coordinate(4.9480737, 52.5286287), new Coordinate(4.9480018, 52.5286182),
                                new Coordinate(4.9479042, 52.5286097), new Coordinate(4.9478304, 52.5286070),
                                new Coordinate(4.9477361, 52.5286077), new Coordinate(4.9476629, 52.5286115),
                                new Coordinate(4.9475901, 52.5286181), new Coordinate(4.9475187, 52.5286274),
                                new Coordinate(4.9474329, 52.5286428), new Coordinate(4.9473646, 52.5286585),
                                new Coordinate(4.9472975, 52.5286772), new Coordinate(4.94726116985194, 52.52868952065891)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9496557, 52.5302944), new Coordinate(4.9493614, 52.5298027),
                                new Coordinate(4.9491665, 52.5295238), new Coordinate(4.9489671, 52.5291903),
                                new Coordinate(4.9489072, 52.5290998), new Coordinate(4.948841, 52.5290184),
                                new Coordinate(4.948786, 52.5289628), new Coordinate(4.9487467, 52.5289261),
                                new Coordinate(4.9487012, 52.5288903), new Coordinate(4.9485991, 52.5288225),
                                new Coordinate(4.948546, 52.5287913), new Coordinate(4.9484652, 52.5287496),
                                new Coordinate(4.9484056, 52.5287231), new Coordinate(4.9483431, 52.5286992),
                                new Coordinate(4.9482489, 52.5286692), new Coordinate(4.9481818, 52.5286513),
                                new Coordinate(4.9480737, 52.5286287), new Coordinate(4.9480018, 52.5286182),
                                new Coordinate(4.9479042, 52.5286097), new Coordinate(4.9478304, 52.528607),
                                new Coordinate(4.9477361, 52.5286077), new Coordinate(4.9476629, 52.5286115),
                                new Coordinate(4.9475901, 52.5286181), new Coordinate(4.9475187, 52.5286274),
                                new Coordinate(4.9474329, 52.5286428), new Coordinate(4.9473646, 52.5286585),
                                new Coordinate(4.9472975, 52.5286772), new Coordinate(4.947194, 52.5287123),
                                new Coordinate(4.9471342, 52.5287371), new Coordinate(4.9470721, 52.5287671),
                                new Coordinate(4.9470187, 52.5287975), new Coordinate(4.9469685, 52.5288304),
                                new Coordinate(4.9469233, 52.5288638), new Coordinate(4.9468804, 52.5288999),
                                new Coordinate(4.946841, 52.5289379), new Coordinate(4.946804, 52.5289793),
                                new Coordinate(4.946773, 52.5290197), new Coordinate(4.9467445, 52.5290635),
                                new Coordinate(4.9467216, 52.5291055), new Coordinate(4.9467055, 52.5291413),
                                new Coordinate(4.9466906, 52.5291854), new Coordinate(4.9466753, 52.5292643),
                                new Coordinate(4.9466726, 52.5293094), new Coordinate(4.9466736, 52.5293544),
                                new Coordinate(4.9466777, 52.5293993), new Coordinate(4.94669, 52.5294738),
                                new Coordinate(4.9466996, 52.5295182), new Coordinate(4.9467119, 52.5295625),
                                new Coordinate(4.9467357, 52.5296331), new Coordinate(4.9467529, 52.5296767),
                                new Coordinate(4.94679, 52.5297577)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(600125593)
                        .reversed(false)
                        .distance(116.64863705130153)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9467900, 52.5297577), new Coordinate(4.9469491, 52.5300568),
                                new Coordinate(4.9472224, 52.5305511), new Coordinate(4.9472481, 52.5307627)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9467900, 52.5297577), new Coordinate(4.9469491, 52.5300568),
                                new Coordinate(4.9472224, 52.5305511), new Coordinate(4.9472481, 52.5307627)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(250409010)
                        .reversed(false)
                        .distance(491.61806977151326)
                        .startFraction(START_FRACTION_0)
                        .endFraction(0.30408706328439755)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9472481, 52.5307627), new Coordinate(4.9485572, 52.5329198),
                                new Coordinate(4.9488666, 52.5334250), new Coordinate(4.9489654, 52.5335943),
                                new Coordinate(4.949348907958022, 52.53422666617821)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(4.9472481, 52.5307627), new Coordinate(4.9485572, 52.5329198),
                                new Coordinate(4.9488666, 52.5334250), new Coordinate(4.9489654, 52.5335943),
                                new Coordinate(4.9493744, 52.5342687), new Coordinate(4.9541982, 52.5422150),
                                new Coordinate(4.9555200, 52.5443876)
                        }))
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
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.18611307333045);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder()
                        .linkId(3666097)
                        .reversed(false)
                        .distance(4.606277878496104)
                        .startFraction(0.8813982849151963)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.431641205942293, 52.17898020844791), new Coordinate(5.431601, 52.178947)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.43194, 52.179227), new Coordinate(5.431601, 52.178947)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666076)
                        .reversed(false)
                        .distance(49.35391266663205)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.431601, 52.178947), new Coordinate(5.431138465119805, 52.17864084147441)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.431601, 52.178947), new Coordinate(5.43111, 52.178622)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666077)
                        .reversed(false)
                        .distance(49.370209885281966)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.43111, 52.178622), new Coordinate(5.431077, 52.1786), new Coordinate(5.430663, 52.178413),
                                new Coordinate(5.430569127715119, 52.178378740042014)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.43111, 52.178622), new Coordinate(5.431077, 52.1786), new Coordinate(5.430663, 52.178413),
                                new Coordinate(5.430526, 52.178363)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666078)
                        .reversed(false)
                        .distance(49.4042427750329)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.430526, 52.178363), new Coordinate(5.430206, 52.178246),
                                new Coordinate(5.429884, 52.178165), new Coordinate(5.429882, 52.178165)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.430526, 52.178363), new Coordinate(5.430206, 52.178246),
                                new Coordinate(5.429884, 52.178165), new Coordinate(5.429882, 52.178165)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666079)
                        .reversed(false)
                        .distance(49.34170254306041)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.429882, 52.178165), new Coordinate(5.429557464435378, 52.17811220888149)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.429882, 52.178165), new Coordinate(5.429507, 52.178104), new Coordinate(5.42918, 52.178064)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666080)
                        .reversed(false)
                        .distance(49.37871322376412)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.42918, 52.178064), new Coordinate(5.4291029, 52.178055),
                                new Coordinate(5.4289015376834, 52.17803320282349)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.42918, 52.178064), new Coordinate(5.4291029, 52.178055),
                                new Coordinate(5.428641, 52.178005), new Coordinate(5.428467, 52.177998)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666081)
                        .reversed(false)
                        .distance(49.3572706954873)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.428467, 52.177998), new Coordinate(5.428057111225008, 52.17798046117762)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.428467, 52.177998), new Coordinate(5.427836, 52.177971), new Coordinate(5.427747, 52.17797)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666082)
                        .reversed(false)
                        .distance(49.39696866946102)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.427747, 52.17797), new Coordinate(5.427079, 52.177963), new Coordinate(5.427025, 52.177964)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.427747, 52.17797), new Coordinate(5.427079, 52.177963), new Coordinate(5.427025, 52.177964)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666083)
                        .reversed(false)
                        .distance(49.37634675570945)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.427025, 52.177964), new Coordinate(5.426949493847171, 52.177965601160466)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.427025, 52.177964), new Coordinate(5.4266949, 52.177971),
                                new Coordinate(5.426323, 52.178013), new Coordinate(5.42631, 52.178015)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666084)
                        .reversed(false)
                        .distance(49.406527048706316)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.42631, 52.178015), new Coordinate(5.426131513721198, 52.17804646994916)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.42631, 52.178015), new Coordinate(5.42593, 52.178082), new Coordinate(5.425628, 52.178159)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666085)
                        .reversed(false)
                        .distance(49.37832995331975)
                        .startFraction(START_FRACTION_0)
                        .endFraction(END_FRACTION_1)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.425628, 52.178159), new Coordinate(5.425557, 52.178177),
                                new Coordinate(5.425238367066216, 52.178298622991996)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.425628, 52.178159), new Coordinate(5.425557, 52.178177),
                                new Coordinate(5.4252269, 52.178303), new Coordinate(5.425037, 52.178409)
                        }))
                        .build(),
                MatchedLink.builder()
                        .linkId(3666086)
                        .reversed(false)
                        .distance(22.597894098636313)
                        .startFraction(START_FRACTION_0)
                        .endFraction(0.4580642228401559)
                        .geometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.425037, 52.178409), new Coordinate(5.4247923939463245, 52.17854550300374)
                        }))
                        .originalGeometry(geometryFactory.createLineString(new Coordinate[]{
                                new Coordinate(5.425037, 52.178409), new Coordinate(5.424503, 52.178707)
                        }))
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


    @SneakyThrows
    @Test
    void match_ok_distance_round_trip() {
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
