package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LineStringLocationDeserializer;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class SinglePointMapMatcherIT {

    private SinglePointMapMatcher singlePointMapMatcher;
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
        GraphHopperSinglePointMapMatcherFactory graphHopperSinglePointMapMatcherFactory =
            new GraphHopperSinglePointMapMatcherFactory(new NetworkGraphHopperFactory());
        singlePointMapMatcher = graphHopperSinglePointMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @SneakyThrows
    @Test
    void testOneWayMatch() {
        // The given point is near a one-way road.
        Point point = geometryFactory.createPoint(new Coordinate(5.427, 52.177));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(point);
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getMatchedLinkIds(), hasSize(1));
        assertThat(singlePointMatch.getSnappedPoints(), hasSize(1));
        assertThat(singlePointMatch.getReliability(), is(66.88548778816732));
    }

    @SneakyThrows
    @Test
    void testBidirectionalWayMatch() {
        // The given point is near a bidirectional road.
        Point point = geometryFactory.createPoint(new Coordinate(5.4280, 52.1798));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(point);
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getMatchedLinkIds(), hasSize(2));
        assertThat(singlePointMatch.getSnappedPoints(), hasSize(1));
        assertThat(singlePointMatch.getReliability(), is(84.57339517842584));
    }

    @SneakyThrows
    @Test
    void testNodeMatch() {
        // The given point is located at the center of a crossroad.
        Point point = geometryFactory.createPoint(new Coordinate(5.426228, 52.18103));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(point);
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getMatchedLinkIds(), hasSize(8));
        assertThat(singlePointMatch.getSnappedPoints(), hasSize(3));
        assertThat(singlePointMatch.getReliability(), is(99.99626435572263));
    }

    @SneakyThrows
    @Test
    void testDoubleMatch() {
        // The given point is near two one-way roads.
        Point point = geometryFactory.createPoint(new Coordinate(5.424633, 52.178623));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(point);
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getMatchedLinkIds(), hasSize(2));
        assertThat(singlePointMatch.getSnappedPoints(), hasSize(2));
        assertThat(singlePointMatch.getReliability(), is(95.46127866694087));
    }

    @SneakyThrows
    @Test
    void testNoMatch() {
        Point point = geometryFactory.createPoint(new Coordinate(5.420, 52.190));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(point);
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.NO_MATCH));
        assertThat(singlePointMatch.getMatchedLinkIds(), hasSize(0));
        assertThat(singlePointMatch.getSnappedPoints(), hasSize(0));
        assertThat(singlePointMatch.getReliability(), is(0.0));
    }
}
