package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LineStringLocationDeserializer;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

class GraphHopperStartToEndMapMatcherIT {

    private StartToEndMapMatcher startToEndMapMatcher;
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
        RoutingNetwork routingNetwork = RoutingNetwork.builder()
                .networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();
        GraphHopperStartToEndMapMatcherFactory graphHopperStartToEndMapMatcherFactory =
                new GraphHopperStartToEndMapMatcherFactory(new NetworkGraphHopperFactory());
        startToEndMapMatcher = graphHopperStartToEndMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
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
        assertThat(lineStringMatch.getMatchedLinkIds()).containsExactly(6405237, 6405238, 6405239, 6405226, 6405227,
                6405228, 6405229, 6405230, 6405231, 6405232, 6405233, 6369284, 6369285, 6369286, 6369287, 6369288);
        assertThat(lineStringMatch.getMatchedLinks()).noneMatch(MatchedLink::isReversed);
        assertThat(lineStringMatch.getUpstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getDownstreamLinkIds()).isNull();
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.6488926754519858);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.9813577658271124);
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
        assertThat(lineStringMatch.getId()).isEqualTo(29);
        assertThat(lineStringMatch.getStatus()).isEqualTo(MatchStatus.MATCH);
        assertThat(lineStringMatch.getReliability()).isEqualTo(93.29643981088304);
        assertThat(lineStringMatch.getLocationIndex()).isEqualTo(-1);
        assertThat(lineStringMatch.isReversed()).isTrue();
        assertThat(lineStringMatch.getMatchedLinkIds()).containsExactly(3666097, 3666076, 3666077, 3666078, 3666079,
                3666080, 3666081, 3666082, 3666083, 3666084, 3666085, 3666086);
        assertThat(lineStringMatch.getMatchedLinks()).noneMatch(MatchedLink::isReversed);
        assertThat(lineStringMatch.getUpstreamLinkIds())
                .containsExactlyInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061);
        assertThat(lineStringMatch.getDownstreamLinkIds())
                .containsExactlyInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217);
        assertThat(lineStringMatch.getStartLinkFraction()).isEqualTo(0.8805534312637381);
        assertThat(lineStringMatch.getEndLinkFraction()).isEqualTo(0.45960570331968187);
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
