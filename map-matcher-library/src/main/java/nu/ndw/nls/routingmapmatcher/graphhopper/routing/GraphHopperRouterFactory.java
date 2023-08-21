package nu.ndw.nls.routingmapmatcher.graphhopper.routing;

import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.RouterFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.AbstractMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;

public class GraphHopperRouterFactory extends AbstractMapMatcherFactory implements RouterFactory {

    public GraphHopperRouterFactory(NetworkGraphHopperFactory networkGraphHopperFactory) {
        super(networkGraphHopperFactory);
    }

    @Override
    public Router createMapMatcher(RoutingNetwork routingNetwork) {
        return new GraphHopperRouter(readNetwork(routingNetwork));
    }

    @Override
    public Router createMapMatcher(Network preInitializedNetwork) {
        return new GraphHopperRouter((NetworkGraphHopper) preInitializedNetwork);
    }
}
