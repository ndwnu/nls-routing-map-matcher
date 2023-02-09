package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingRange;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocationWithBearing;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class SinglePointMapMatcherWithBearingIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";

    private SinglePointMapMatcher singlePointMapMatcher;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        String linksJson = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(
                        SinglePointMapMatcherWithBearingIT.LINKS_RESOURCE)),
                StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder().networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();
        GraphHopperSinglePointMapMatcherFactory graphHopperSinglePointMapMatcherFactory =
                new GraphHopperSinglePointMapMatcherFactory(new NetworkGraphHopperFactory());
        singlePointMapMatcher = graphHopperSinglePointMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @Test
    void matchWithBearing_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(new Coordinate(5.426747, 52.176663));
        SinglePointLocationWithBearing request = new SinglePointLocationWithBearing(1, point,
                new BearingRange(130.0, 140.0),20.0);
        SinglePointMatch result = singlePointMapMatcher.matchWithBearing(request);
        assertThat(result.getCandidateMatches(), hasSize(1));
        CandidateMatch match = result.getCandidateMatches().get(0);
        assertThat(match.getMatchedLinkId(), is(3667044));
        assertThat(match.getSnappedPoint().getX(), is(5.426768463894968));
        assertThat(match.getSnappedPoint().getY(), is(52.176694564551426));
        assertThat(match.getDistance(), is(3.8067685587693947));
        assertThat(match.getFraction(), is(0.7627151428527583));
    }

    @Test
    void matchWithBearing_with_snapped_point_at_end_ok() {
        setupNetwork();
        Point point = geometryFactory.createPoint(new Coordinate(5.424288905298937,52.17787309176586));
        SinglePointLocationWithBearing request = new SinglePointLocationWithBearing(1, point,
                new BearingRange(188,14),250D);
        SinglePointMatch result = singlePointMapMatcher.matchWithBearing(request);
        assertThat(result.getCandidateMatches(), hasSize(10));
//        CandidateMatch match = result.getCandidateMatches().get(0);
//        assertThat(match.getMatchedLinkId(), is(3667044));
//        assertThat(match.getSnappedPoint().getX(), is(5.426768463894968));
//        assertThat(match.getSnappedPoint().getY(), is(52.176694564551426));
//        assertThat(match.getDistance(), is(3.8067685587693947));
//        assertThat(match.getFraction(), is(0.7627151428527583));
    }
}
