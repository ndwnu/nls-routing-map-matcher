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
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingProfile;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

class GraphHopperRouterIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private Router router;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork() {
        String linksJson = IOUtils.toString(Objects.requireNonNull(getClass().getResourceAsStream(
                        LINKS_RESOURCE)),
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
                .wayPoints(wayPoints).build()
        );
        assertThat(result.getStartLinkFraction()).isEqualTo(0.8236516616727612);
        assertThat(result.getEndLinkFraction()).isEqualTo(0.5228504089301351);
        assertThat(result.getDistance()).isEqualTo(243.6);
        assertThat(result.getMatchedLinkIds()).isEqualTo(
                List.of(7223072, 7223073, 3667130, 3667131, 3667132, 3667133, 3666204));
    }
}
