package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingProfile;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

class GraphHopperRouterIT {

    private Router router;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        String linksJson = IOUtils.toString(
                Objects.requireNonNull(getClass().getResourceAsStream("/test-data/links.json")),
                StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });
        RoutingNetwork routingNetwork = RoutingNetwork.builder().networkNameAndVersion("test_network")
                .linkSupplier(links::iterator).build();
        GraphHopperRouterFactory graphHopperSinglePointMapMatcherFactory =
                new GraphHopperRouterFactory(new NetworkGraphHopperFactory());
        router = graphHopperSinglePointMapMatcherFactory.createMapMatcher(routingNetwork);
        geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
    }

    @SneakyThrows
    @Test
    void route_ok() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile(RoutingProfile.CAR_FASTEST)
                .wayPoints(wayPoints)
                .build());
        assertSuccess(result, new Coordinate[]{new Coordinate(5.430483, 52.177693),
                new Coordinate(5.428436, 52.175901)});
    }

    @SneakyThrows
    @Test
    void route_ok_noSimplify() {
        setupNetwork();
        Point start = geometryFactory.createPoint(new Coordinate(5.430496, 52.177687));
        Point end = geometryFactory.createPoint(new Coordinate(5.428436, 52.175901));
        List<Point> wayPoints = List.of(start, end);
        var result = router.route(RoutingRequest.builder()
                .routingProfile(RoutingProfile.CAR_FASTEST)
                .wayPoints(wayPoints)
                .simplifyResponseGeometry(false)
                .build());
        assertSuccess(result, new Coordinate[]{new Coordinate(5.430483, 52.177693), new Coordinate(5.430413, 52.177631),
                new Coordinate(5.430015, 52.17728), new Coordinate(5.429664, 52.176974),
                new Coordinate(5.429312, 52.176668), new Coordinate(5.428961, 52.176362),
                new Coordinate(5.42861, 52.176056), new Coordinate(5.428436, 52.175901)});
    }

    private void assertSuccess(RoutingResponse result, Coordinate[] coordinates) {
        assertThat(result.getStartLinkFraction()).isEqualTo(0.8236516616727612);
        assertThat(result.getEndLinkFraction()).isEqualTo(0.5228504089301351);
        assertThat(result.getMatchedLinkIds())
                .containsExactly(7223072, 7223073, 3667130, 3667131, 3667132, 3667133, 3666204);
        assertThat(result.getMatchedLinks()).noneMatch(MatchedLink::isReversed);
        assertThat(result.getGeometry()).isEqualTo(geometryFactory.createLineString(coordinates));
        assertThat(result.getWeight()).isEqualTo(8.769);
        assertThat(result.getDuration()).isEqualTo(8.768);
        assertThat(result.getDistance()).isEqualTo(243.592);
    }
}
