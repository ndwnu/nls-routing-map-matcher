package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

class ViterbiLineStringMapMatcherIT {

    private LineStringMapMatcher viterbiLineStringMapMatcher;
    private ObjectMapper mapper;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    @BeforeEach
    void setup() {
        String linksJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/links.json")),
                StandardCharsets.UTF_8);
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder().networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();
        ViterbiLinestringMapMatcherFactory viterbiLinestringMapMatcherFactory = new ViterbiLinestringMapMatcherFactory(
                new NetworkGraphHopperFactory());
        viterbiLineStringMapMatcher = viterbiLinestringMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @SneakyThrows
    @Test
    void match_ok() {
        String locationJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/matched_linestring_location.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertSuccess(lineStringMatch,
                new Coordinate[]{new Coordinate(5.431641, 52.17898), new Coordinate(5.431601, 52.178947),
                        new Coordinate(5.431138, 52.178641), new Coordinate(5.43111, 52.178622),
                        new Coordinate(5.431077, 52.1786), new Coordinate(5.430663, 52.178413),
                        new Coordinate(5.430569, 52.178379), new Coordinate(5.430526, 52.178363),
                        new Coordinate(5.430206, 52.178246), new Coordinate(5.429884, 52.178165),
                        new Coordinate(5.429882, 52.178165), new Coordinate(5.429557, 52.178112),
                        new Coordinate(5.429507, 52.178104), new Coordinate(5.42918, 52.178064),
                        new Coordinate(5.429103, 52.178055), new Coordinate(5.428902, 52.178033),
                        new Coordinate(5.428641, 52.178005), new Coordinate(5.428467, 52.177998),
                        new Coordinate(5.428057, 52.17798), new Coordinate(5.427836, 52.177971),
                        new Coordinate(5.427747, 52.17797), new Coordinate(5.427079, 52.177963),
                        new Coordinate(5.427025, 52.177964), new Coordinate(5.426949, 52.177966),
                        new Coordinate(5.426695, 52.177971), new Coordinate(5.426323, 52.178013),
                        new Coordinate(5.42631, 52.178015), new Coordinate(5.426132, 52.178046),
                        new Coordinate(5.42593, 52.178082), new Coordinate(5.425628, 52.178159),
                        new Coordinate(5.425557, 52.178177), new Coordinate(5.425238, 52.178299),
                        new Coordinate(5.425227, 52.178303), new Coordinate(5.425037, 52.178409),
                        new Coordinate(5.424792, 52.178546)});
    }

    @SneakyThrows
    @Test
    void match_ok_with_double_end() {
        String locationJson = IOUtils.toString(Objects.requireNonNull(
                        getClass().getResourceAsStream("/test-data/matched_linestring_location_double_end.json")),
                StandardCharsets.UTF_8);
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(0);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinks()).containsExactly(
                MatchedLink.builder().linkId(600767674).reversed(false).build(),
                MatchedLink.builder().linkId(252408103).reversed(false).build(),
                MatchedLink.builder().linkId(252408066).reversed(false).build(),
                MatchedLink.builder().linkId(600125366).reversed(false).build(),
                MatchedLink.builder().linkId(600126141).reversed(false).build(),
                MatchedLink.builder().linkId(600126144).reversed(false).build(),
                MatchedLink.builder().linkId(600126143).reversed(false).build(),
                MatchedLink.builder().linkId(600126037).reversed(false).build(),
                MatchedLink.builder().linkId(600125593).reversed(false).build(),
                MatchedLink.builder().linkId(250409010).reversed(false).build());
        assertNull(lineStringMatch.getUpstreamLinkIds());
        assertNull(lineStringMatch.getDownstreamLinkIds());
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.6608445201176048);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.3047612754692782);
        assertThat(lineStringMatch.getLineString()).isEqualTo(geometryFactory.createLineString(
                new Coordinate[]{new Coordinate(4.964447, 52.524932), new Coordinate(4.96461, 52.525195),
                        new Coordinate(4.964734, 52.525401), new Coordinate(4.964807, 52.525523),
                        new Coordinate(4.964892, 52.525656), new Coordinate(4.964948, 52.52575),
                        new Coordinate(4.965033, 52.525877), new Coordinate(4.965045, 52.525889),
                        new Coordinate(4.965057, 52.525897), new Coordinate(4.965098, 52.525919),
                        new Coordinate(4.965148, 52.525983), new Coordinate(4.965571, 52.526675),
                        new Coordinate(4.965621, 52.526737), new Coordinate(4.965626, 52.526877),
                        new Coordinate(4.965279, 52.526952), new Coordinate(4.965211, 52.52697),
                        new Coordinate(4.965142, 52.526985), new Coordinate(4.965073, 52.527001),
                        new Coordinate(4.965004, 52.527016), new Coordinate(4.964935, 52.527032),
                        new Coordinate(4.964866, 52.527047), new Coordinate(4.964796, 52.527062),
                        new Coordinate(4.964727, 52.527078), new Coordinate(4.964658, 52.527093),
                        new Coordinate(4.964589, 52.527109), new Coordinate(4.96452, 52.527125),
                        new Coordinate(4.964452, 52.527141), new Coordinate(4.964349, 52.527165),
                        new Coordinate(4.964314, 52.527173), new Coordinate(4.964245, 52.527189),
                        new Coordinate(4.964177, 52.527205), new Coordinate(4.964108, 52.527222),
                        new Coordinate(4.964039, 52.527238), new Coordinate(4.963971, 52.527254),
                        new Coordinate(4.963902, 52.527271), new Coordinate(4.963834, 52.527287),
                        new Coordinate(4.963766, 52.527304), new Coordinate(4.963697, 52.527321),
                        new Coordinate(4.963629, 52.527337), new Coordinate(4.963561, 52.527354),
                        new Coordinate(4.963492, 52.527371), new Coordinate(4.963424, 52.527387),
                        new Coordinate(4.963356, 52.527404), new Coordinate(4.963288, 52.527421),
                        new Coordinate(4.96322, 52.527439), new Coordinate(4.963152, 52.527456),
                        new Coordinate(4.963084, 52.527473), new Coordinate(4.962962, 52.527503),
                        new Coordinate(4.962947, 52.527507), new Coordinate(4.962879, 52.527524),
                        new Coordinate(4.962811, 52.52754), new Coordinate(4.962742, 52.527557),
                        new Coordinate(4.962674, 52.527573), new Coordinate(4.962605, 52.52759),
                        new Coordinate(4.962537, 52.527606), new Coordinate(4.962468, 52.527623),
                        new Coordinate(4.9624, 52.527639), new Coordinate(4.962331, 52.527656),
                        new Coordinate(4.962263, 52.527672), new Coordinate(4.962194, 52.527688),
                        new Coordinate(4.962116, 52.527706), new Coordinate(4.962056, 52.52772),
                        new Coordinate(4.961987, 52.527735), new Coordinate(4.961918, 52.527751),
                        new Coordinate(4.961849, 52.527766), new Coordinate(4.96178, 52.527781),
                        new Coordinate(4.961711, 52.527797), new Coordinate(4.961642, 52.527812),
                        new Coordinate(4.961572, 52.527828), new Coordinate(4.961503, 52.527843),
                        new Coordinate(4.961434, 52.527858), new Coordinate(4.961365, 52.527874),
                        new Coordinate(4.961296, 52.527889), new Coordinate(4.961227, 52.527904),
                        new Coordinate(4.961206, 52.527909), new Coordinate(4.961157, 52.52792),
                        new Coordinate(4.961088, 52.527935), new Coordinate(4.961019, 52.52795),
                        new Coordinate(4.96095, 52.527966), new Coordinate(4.960881, 52.527981),
                        new Coordinate(4.960811, 52.527996), new Coordinate(4.960742, 52.528012),
                        new Coordinate(4.960673, 52.528027), new Coordinate(4.960604, 52.528042),
                        new Coordinate(4.960535, 52.528058), new Coordinate(4.960466, 52.528073),
                        new Coordinate(4.960397, 52.528088), new Coordinate(4.960327, 52.528104),
                        new Coordinate(4.960258, 52.528119), new Coordinate(4.960189, 52.528134),
                        new Coordinate(4.96012, 52.52815), new Coordinate(4.960051, 52.528165),
                        new Coordinate(4.959981, 52.528181), new Coordinate(4.959912, 52.528196),
                        new Coordinate(4.959843, 52.528212), new Coordinate(4.959774, 52.528227),
                        new Coordinate(4.959705, 52.528243), new Coordinate(4.959636, 52.528258),
                        new Coordinate(4.959567, 52.528274), new Coordinate(4.959498, 52.528289),
                        new Coordinate(4.959429, 52.528305), new Coordinate(4.95936, 52.52832),
                        new Coordinate(4.959291, 52.528336), new Coordinate(4.959265, 52.528342),
                        new Coordinate(4.959222, 52.528352), new Coordinate(4.959153, 52.528367),
                        new Coordinate(4.959084, 52.528383), new Coordinate(4.959015, 52.528398),
                        new Coordinate(4.958946, 52.528414), new Coordinate(4.958877, 52.528429),
                        new Coordinate(4.958808, 52.528445), new Coordinate(4.958739, 52.52846),
                        new Coordinate(4.958669, 52.528476), new Coordinate(4.9586, 52.528491),
                        new Coordinate(4.958531, 52.528506), new Coordinate(4.958462, 52.528522),
                        new Coordinate(4.958393, 52.528537), new Coordinate(4.958324, 52.528553),
                        new Coordinate(4.958255, 52.528568), new Coordinate(4.958185, 52.528583),
                        new Coordinate(4.958116, 52.528599), new Coordinate(4.958047, 52.528614),
                        new Coordinate(4.957978, 52.52863), new Coordinate(4.957909, 52.528645),
                        new Coordinate(4.95784, 52.52866), new Coordinate(4.957771, 52.528676),
                        new Coordinate(4.957701, 52.528691), new Coordinate(4.957434, 52.528768),
                        new Coordinate(4.957365, 52.528784), new Coordinate(4.957296, 52.528799),
                        new Coordinate(4.957227, 52.528814), new Coordinate(4.957158, 52.52883),
                        new Coordinate(4.957088, 52.528845), new Coordinate(4.95695, 52.528876),
                        new Coordinate(4.956812, 52.528907), new Coordinate(4.956743, 52.528922),
                        new Coordinate(4.956674, 52.528938), new Coordinate(4.956605, 52.528953),
                        new Coordinate(4.956536, 52.528968), new Coordinate(4.956398, 52.528999),
                        new Coordinate(4.956182, 52.52903), new Coordinate(4.956113, 52.529045),
                        new Coordinate(4.956044, 52.52906), new Coordinate(4.955975, 52.529076),
                        new Coordinate(4.955906, 52.529091), new Coordinate(4.955837, 52.529107),
                        new Coordinate(4.955768, 52.529122), new Coordinate(4.955699, 52.529138),
                        new Coordinate(4.95563, 52.529153), new Coordinate(4.955561, 52.529168),
                        new Coordinate(4.955492, 52.529184), new Coordinate(4.955423, 52.529199),
                        new Coordinate(4.955354, 52.529214), new Coordinate(4.955285, 52.529229),
                        new Coordinate(4.955216, 52.529245), new Coordinate(4.955147, 52.52926),
                        new Coordinate(4.955078, 52.529275), new Coordinate(4.955009, 52.52929),
                        new Coordinate(4.95494, 52.529306), new Coordinate(4.954871, 52.529321),
                        new Coordinate(4.954802, 52.529336), new Coordinate(4.954733, 52.529352),
                        new Coordinate(4.954663, 52.529367), new Coordinate(4.954595, 52.529382),
                        new Coordinate(4.954526, 52.529398), new Coordinate(4.954457, 52.529413),
                        new Coordinate(4.954388, 52.529428), new Coordinate(4.954319, 52.529444),
                        new Coordinate(4.95425, 52.529459), new Coordinate(4.954181, 52.529475),
                        new Coordinate(4.954112, 52.52949), new Coordinate(4.954043, 52.529505),
                        new Coordinate(4.953974, 52.529521), new Coordinate(4.953905, 52.529537),
                        new Coordinate(4.951275, 52.530163), new Coordinate(4.951236, 52.530173),
                        new Coordinate(4.951169, 52.530192), new Coordinate(4.951103, 52.530211),
                        new Coordinate(4.951037, 52.530231), new Coordinate(4.950971, 52.53025),
                        new Coordinate(4.950904, 52.530269), new Coordinate(4.950837, 52.530287),
                        new Coordinate(4.95077, 52.530305), new Coordinate(4.950736, 52.530314),
                        new Coordinate(4.950702, 52.530323), new Coordinate(4.950635, 52.530341),
                        new Coordinate(4.950567, 52.530359), new Coordinate(4.9505, 52.530377),
                        new Coordinate(4.950433, 52.530395), new Coordinate(4.950365, 52.530413),
                        new Coordinate(4.950297, 52.530429), new Coordinate(4.949973, 52.530536),
                        new Coordinate(4.949909, 52.530553), new Coordinate(4.949839, 52.530572),
                        new Coordinate(4.949656, 52.530294), new Coordinate(4.949361, 52.529803),
                        new Coordinate(4.949167, 52.529524), new Coordinate(4.948967, 52.52919),
                        new Coordinate(4.948907, 52.5291), new Coordinate(4.948841, 52.529018),
                        new Coordinate(4.948786, 52.528963), new Coordinate(4.948747, 52.528926),
                        new Coordinate(4.948701, 52.52889), new Coordinate(4.948599, 52.528823),
                        new Coordinate(4.948546, 52.528791), new Coordinate(4.948465, 52.52875),
                        new Coordinate(4.948406, 52.528723), new Coordinate(4.948343, 52.528699),
                        new Coordinate(4.948249, 52.528669), new Coordinate(4.948182, 52.528651),
                        new Coordinate(4.948074, 52.528629), new Coordinate(4.948002, 52.528618),
                        new Coordinate(4.947904, 52.52861), new Coordinate(4.94783, 52.528607),
                        new Coordinate(4.947736, 52.528608), new Coordinate(4.947663, 52.528612),
                        new Coordinate(4.94759, 52.528618), new Coordinate(4.947519, 52.528627),
                        new Coordinate(4.947433, 52.528643), new Coordinate(4.947365, 52.528659),
                        new Coordinate(4.947298, 52.528677), new Coordinate(4.947261, 52.52869),
                        new Coordinate(4.947194, 52.528712), new Coordinate(4.947134, 52.528737),
                        new Coordinate(4.947072, 52.528767), new Coordinate(4.947019, 52.528798),
                        new Coordinate(4.946969, 52.52883), new Coordinate(4.946923, 52.528864),
                        new Coordinate(4.94688, 52.5289), new Coordinate(4.946841, 52.528938),
                        new Coordinate(4.946804, 52.528979), new Coordinate(4.946773, 52.52902),
                        new Coordinate(4.946745, 52.529064), new Coordinate(4.946722, 52.529106),
                        new Coordinate(4.946706, 52.529141), new Coordinate(4.946691, 52.529185),
                        new Coordinate(4.946675, 52.529264), new Coordinate(4.946673, 52.529309),
                        new Coordinate(4.946674, 52.529354), new Coordinate(4.946678, 52.529399),
                        new Coordinate(4.94669, 52.529474), new Coordinate(4.9467, 52.529518),
                        new Coordinate(4.946712, 52.529563), new Coordinate(4.946736, 52.529633),
                        new Coordinate(4.946753, 52.529677), new Coordinate(4.94679, 52.529758),
                        new Coordinate(4.946949, 52.530057), new Coordinate(4.947222, 52.530551),
                        new Coordinate(4.947248, 52.530763), new Coordinate(4.948557, 52.53292),
                        new Coordinate(4.948867, 52.533425), new Coordinate(4.948965, 52.533594),
                        new Coordinate(4.949349, 52.534227), new Coordinate(4.949374, 52.534269),
                        new Coordinate(4.949761, 52.534906)}));
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
        LineStringLocation lineStringLocation = LineStringLocation.builder().id(l.getId())
                .upstreamIsochrone(l.getUpstreamIsochrone()).upstreamIsochroneUnit(l.getUpstreamIsochroneUnit())
                .downstreamIsochrone(l.getDownstreamIsochrone()).downstreamIsochroneUnit(l.getDownstreamIsochroneUnit())
                .locationIndex(l.getLocationIndex()).reversed(l.isReversed()).lengthInMeters(l.getLengthInMeters())
                .geometry(l.getGeometry()).reliabilityCalculationType(l.getReliabilityCalculationType())
                .radius(l.getRadius()).simplifyResponseGeometry(true).build();

        LineStringMatch lineStringMatch = viterbiLineStringMapMatcher.match(lineStringLocation);
        assertSuccess(lineStringMatch,
                new Coordinate[]{new Coordinate(5.431641, 52.17898), new Coordinate(5.431077, 52.1786),
                        new Coordinate(5.430663, 52.178413), new Coordinate(5.430206, 52.178246),
                        new Coordinate(5.429884, 52.178165), new Coordinate(5.429507, 52.178104),
                        new Coordinate(5.428641, 52.178005), new Coordinate(5.427836, 52.177971),
                        new Coordinate(5.427079, 52.177963), new Coordinate(5.426695, 52.177971),
                        new Coordinate(5.426323, 52.178013), new Coordinate(5.42593, 52.178082),
                        new Coordinate(5.425557, 52.178177), new Coordinate(5.425227, 52.178303),
                        new Coordinate(5.424792, 52.178546)});
    }

    private void assertSuccess(LineStringMatch lineStringMatch, Coordinate[] coordinates) {
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.18611307333045);
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
        assertThat(lineStringMatch.getUpstreamLinkIds()).containsExactlyInAnyOrder(3666097, 3666096, 3666095, 3666094,
                7223062, 7223061);
        assertThat(lineStringMatch.getDownstreamLinkIds()).containsExactlyInAnyOrder(3666086, 3666105, 3666106, 3666107,
                3666108, 3666109, 3686216, 3686217);
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.8805534312637381);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.45960570331968187);
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
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(0);
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
