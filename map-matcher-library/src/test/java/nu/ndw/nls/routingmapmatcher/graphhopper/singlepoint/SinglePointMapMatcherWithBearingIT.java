package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocationWithBearing;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;

public class SinglePointMapMatcherWithBearingIT {

    private static final String LINKS_RESOURCE = "/test-data/links.json";
    private static final String SHIVI_LINKS_RESOURCE = "/test-data/shivi-verkeersbanen.json";
    private static final int ID = 123;

    private SinglePointMapMatcher singlePointMapMatcher;
    private ObjectMapper mapper;
    private GeometryFactory geometryFactory;

    @SneakyThrows
    private void setupNetwork(String resource) {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream(resource));
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
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

    @Test
    void matchWithBearing_ok(){
        setupNetwork(LINKS_RESOURCE);
        Point point = geometryFactory.createPoint(new Coordinate(5.426747, 52.176663));
        var request = new SinglePointLocationWithBearing(1,point,List.of(310.0,320.0),20.0);
        singlePointMapMatcher.matchWithBearing(request);
    }

}
