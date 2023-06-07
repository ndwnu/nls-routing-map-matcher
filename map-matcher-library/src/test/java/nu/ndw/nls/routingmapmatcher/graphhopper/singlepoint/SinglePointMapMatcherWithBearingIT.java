package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static org.hamcrest.MatcherAssert.assertThat;
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
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.BearingFilter;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

@TestInstance(Lifecycle.PER_CLASS)
public class SinglePointMapMatcherWithBearingIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private SinglePointMapMatcher singlePointMapMatcher;

    @SneakyThrows
    private void setupNetwork() {
        String linksJson = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(
                        SinglePointMapMatcherWithBearingIT.LINKS_RESOURCE)), StandardCharsets.UTF_8);
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
    }

    @BeforeAll
    void beforeAll() {
        setupNetwork();
    }

    @Test
    void match_ok_withBearingFilter() {
        SinglePointLocation request = SinglePointLocation.builder()
                .id(1)
                .point(createPoint(5.426747, 52.176663))
                .cutoffDistance(10.0)
                .bearingFilter(new BearingFilter(135, 5))
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);

        assertThat(result, is(SinglePointMatch.builder()
                .id(1)
                .status(MatchStatus.MATCH)
                .reliability(4.263311114206914)
                .candidateMatches(List.of(SinglePointMatch.CandidateMatch.builder()
                        .matchedLinkId(3667044)
                        .snappedPoint(createPoint(5.426768463894968, 52.176694564551426))
                        .fraction(0.7627151428741638)
                        .distance(3.8067685587693947)
                        .bearing(137.88345016490496)
                        .reliability(4.263311114206914)
                        .build()))
                .build()));
    }

    @Test
    void match_ok_withBearingFilterAndSnappedPointAtEnd() {
        SinglePointLocation request = SinglePointLocation.builder()
                .id(1)
                .point(createPoint(5.424289, 52.177873))
                .cutoffDistance(25.0)
                .bearingFilter(new BearingFilter(160, 5))
                .build();
        SinglePointMatch result = singlePointMapMatcher.match(request);
        assertThat(result, is(SinglePointMatch.builder()
                .id(1)
                .status(MatchStatus.MATCH)
                .reliability(56.44987281010212)
                .candidateMatches(List.of(
                        SinglePointMatch.CandidateMatch.builder()
                                .matchedLinkId(3667015)
                                .snappedPoint(createPoint(5.424354551625818, 52.177909791821605))
                                .fraction(0.3953371628360206)
                                .distance(6.0717940825069165)
                                .bearing(160.9631475429935)
                                .reliability(56.44987281010212)
                                .build(),
                        SinglePointMatch.CandidateMatch.builder()
                                .matchedLinkId(3667014)
                                .snappedPoint(createPoint(5.424268, 52.178064))
                                .fraction(1)
                                .distance(21.301202848581156)
                                .bearing(161.06168302121978)
                                .reliability(0)
                                .build()))
                .build()));
    }

    private static Point createPoint(double x, double y) {
        return GlobalConstants.WGS84_GEOMETRY_FACTORY.createPoint(new Coordinate(x, y));
    }
}
