package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
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
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(1));
        assertThatUpstreamAndDownstreamAreNull(singlePointMatch);
        assertThat(getSnappedPoints(singlePointMatch), hasSize(1));
        assertThat(singlePointMatch.getReliability(), is(66.88548778816732));
    }

    @SneakyThrows
    @Test
    void testBidirectionalWayMatch() {
        // The given point is near a bidirectional road.
        Point point = geometryFactory.createPoint(new Coordinate(5.4280, 52.1798));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(2));
        assertThatUpstreamAndDownstreamAreNull(singlePointMatch);
        assertThat(getSnappedPoints(singlePointMatch), hasSize(1));
        assertThat(singlePointMatch.getReliability(), is(84.57339517842584));
    }

    @SneakyThrows
    @Test
    void testNodeMatch() {
        // The given point is located at the center of a crossroad.
        Point point = geometryFactory.createPoint(new Coordinate(5.426228, 52.18103));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(8));
        assertThatUpstreamAndDownstreamAreNull(singlePointMatch);
        assertThat(getSnappedPoints(singlePointMatch), hasSize(3));
        assertThat(singlePointMatch.getReliability(), is(99.99626435572263));
    }

    @SneakyThrows
    @Test
    void testDoubleMatch() {
        // The given point is near two one-way roads.
        Point point = geometryFactory.createPoint(new Coordinate(5.424633, 52.178623));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(2));
        assertThatUpstreamAndDownstreamAreNull(singlePointMatch);
        assertThat(getSnappedPoints(singlePointMatch), hasSize(2));
        assertThat(singlePointMatch.getReliability(), is(95.46127866694087));
    }

    @SneakyThrows
    @Test
    void testNoMatch() {
        Point point = geometryFactory.createPoint(new Coordinate(5.420, 52.190));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.NO_MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(0));
        assertThat(singlePointMatch.getReliability(), is(0.0));
    }

    @SneakyThrows
    @Test
    void testUpstreamDownstream() {
        Point point = geometryFactory.createPoint(new Coordinate(5.4278, 52.1764));
        SinglePointMatch singlePointMatch = singlePointMapMatcher.match(new SinglePointLocation(point, 1000,
                IsochroneUnit.METERS, 30, IsochroneUnit.SECONDS));
        assertThat(singlePointMatch, is(notNullValue()));
        assertThat(singlePointMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(singlePointMatch.getCandidateMatches(), hasSize(1));
        assertThat(getSnappedPoints(singlePointMatch), hasSize(1));
        assertThat(singlePointMatch.getReliability(), is(92.439358238897));

        SinglePointMatch.CandidateMatch candidateMatch = singlePointMatch.getCandidateMatches().get(0);
        assertThat(candidateMatch.getMatchedLinkId(), is(3666958));
        // TODO upstream and downstream shouldn't contain matched segment itself?
        assertThat(candidateMatch.getUpstreamLinkIds(), containsInAnyOrder(3666958,
                3666957, 3666956, 3666955, 3667003, 3667002, 3667001, 3667000, 3666999, 3666998, 3666997, 3666996,
                3666256, 3666973, 3666972, 3666971, 3666970, 3666969, 3666968, 3666967, 3666966, 3666974, 3667137,
                3667136, 3667135, 3667134, 3666244, 3666243, 3666242, 3666241, 3666240, 3666223, 3667125, 3667124,
                3667123, 3667122, 3667121, 3667120));
        assertThat(candidateMatch.getDownstreamLinkIds(), containsInAnyOrder(3666958,
                3666098, 3666099, 3666100, 3666101, 3666102, 3666103, 3666104, 3666105, 3666106, 3666107, 3666108,
                3666109, 3686216, 3686217, 3666945, 3666946, 3666947, 3666948, 3666949, 3666950, 3666951, 3666952,
                3666943, 3666944, 3666953, 3666954, 3666123, 3666110, 3666111, 3666112, 3666113, 3666114, 3666130,
                3666115, 3666116, 3666117, 3666118, 3666119, 3666120));
    }

    private void assertThatUpstreamAndDownstreamAreNull(SinglePointMatch singlePointMatch) {
        for (SinglePointMatch.CandidateMatch candidateMatch : singlePointMatch.getCandidateMatches()) {
            assertThat(candidateMatch.getUpstreamLinkIds(), is(nullValue()));
            assertThat(candidateMatch.getDownstreamLinkIds(), is(nullValue()));
        }
    }

    private Set<Point> getSnappedPoints(SinglePointMatch singlePointMatch) {
        return singlePointMatch.getCandidateMatches().stream()
                .map(SinglePointMatch.CandidateMatch::getSnappedPoint)
                .collect(Collectors.toSet());
    }
}
