package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import com.graphhopper.routing.QueryGraphExtractor;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.RouterFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.AbstractMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

public class GraphHopperRouterFactory extends AbstractMapMatcherFactory implements RouterFactory {

    public GraphHopperRouterFactory(
            NetworkGraphHopperFactory networkGraphHopperFactory) {
        super(networkGraphHopperFactory);
    }

    @Override
    public Router createMapMatcher(RoutingNetwork routingNetwork) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        return new GraphHopperRouter(readNetwork(routingNetwork),
                new QueryGraphExtractor(), new PathUtil(geometryFactory));
    }

    @Override
    public Router createMapMatcher(Network preInitializedNetwork) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        return new GraphHopperRouter((NetworkGraphHopper) preInitializedNetwork, new QueryGraphExtractor(),
                new PathUtil(geometryFactory));
    }
}
