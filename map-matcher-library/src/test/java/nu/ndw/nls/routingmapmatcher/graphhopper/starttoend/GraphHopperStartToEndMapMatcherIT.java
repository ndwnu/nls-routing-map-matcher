package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
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
    private void setup() {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
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
        assertThat(lineStringMatch, is(notNullValue()));
        assertThat(lineStringMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(lineStringMatch.getMatchedLinkIds(), hasSize(16));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.6486691616943794));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.9814440510788827));
        assertThat(lineStringMatch.getReliability(), is(73.89369696136275));
    }

    @SneakyThrows
    @Test
    void match_ok_lineStringWithIsochrones() {
        String locationJson = IOUtils.toString(
                getClass().getResourceAsStream("/test-data/matched_linestring_location.json"));
        LineStringLocation lineStringLocation = mapper.readValue(locationJson, LineStringLocation.class);
        LineStringMatch lineStringMatch = startToEndMapMatcher.match(lineStringLocation);
        assertThat(lineStringMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(lineStringMatch.getMatchedLinkIds(), contains(3666097, 3666076, 3666077, 3666078, 3666079, 3666080,
                3666081, 3666082, 3666083, 3666084, 3666085, 3666086));
        assertThat(lineStringMatch.getUpstreamLinkIds(),
                containsInAnyOrder(3666097, 3666096, 3666095, 3666094, 7223062, 7223061));
        assertThat(lineStringMatch.getDownstreamLinkIds(),
                containsInAnyOrder(3666086, 3666105, 3666106, 3666107, 3666108, 3666109, 3686216, 3686217));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.8802584207113416));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.45984987610479167));
        assertThat(lineStringMatch.getReliability(), is(93.31281800491443));
        assertThat(lineStringMatch.getId(), is(29));
        assertThat(lineStringMatch.getLocationIndex(), is(-1));
        assertThat(lineStringMatch.isReversed(), is(true));
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
        assertThat(lineStringMatch, is(notNullValue()));
        assertThat(lineStringMatch.getStatus(), is(MatchStatus.NO_MATCH));
        assertThat(lineStringMatch.getMatchedLinkIds(), hasSize(0));
        assertThat(lineStringMatch.getStartLinkFraction(), is(0.0));
        assertThat(lineStringMatch.getEndLinkFraction(), is(0.0));
        assertThat(lineStringMatch.getReliability(), is(0.0));
    }
}
