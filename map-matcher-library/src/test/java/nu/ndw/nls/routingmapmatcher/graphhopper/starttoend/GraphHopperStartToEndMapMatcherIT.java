package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndMatch;
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

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

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
    void testMatch() {
        Point startPoint = geometryFactory.createPoint(new Coordinate(5.431, 52.181));
        Point endPoint = geometryFactory.createPoint(new Coordinate(5.423, 52.181));
        StartToEndLocation startToEndLocation = new StartToEndLocation(1, 1, 543.0, startPoint, endPoint);
        StartToEndMatch startToEndMatch = startToEndMapMatcher.match(startToEndLocation);
        assertThat(startToEndMatch, is(notNullValue()));
        assertThat(startToEndMatch.getStatus(), is(MatchStatus.MATCH));
        assertThat(startToEndMatch.getMatchedLinkIds(), hasSize(16));
        assertThat(startToEndMatch.getStartLinkFraction(), is(0.6486691616943794));
        assertThat(startToEndMatch.getEndLinkFraction(), is(0.9814440510788827));
        assertThat(startToEndMatch.getReliability(), is(73.89369696136275));
    }

    @SneakyThrows
    @Test
    void testNoMatch() {
        Point startPoint = geometryFactory.createPoint(new Coordinate(5.430, 52.180));
        Point endPoint = geometryFactory.createPoint(new Coordinate(5.431, 52.181));
        StartToEndLocation startToEndLocation = new StartToEndLocation(1, 1, 130.0, startPoint, endPoint);
        StartToEndMatch startToEndMatch = startToEndMapMatcher.match(startToEndLocation);
        assertThat(startToEndMatch, is(notNullValue()));
        assertThat(startToEndMatch.getStatus(), is(MatchStatus.NO_MATCH));
        assertThat(startToEndMatch.getMatchedLinkIds(), hasSize(0));
        assertThat(startToEndMatch.getStartLinkFraction(), is(0.0));
        assertThat(startToEndMatch.getEndLinkFraction(), is(0.0));
        assertThat(startToEndMatch.getReliability(), is(0.0));
    }
}
