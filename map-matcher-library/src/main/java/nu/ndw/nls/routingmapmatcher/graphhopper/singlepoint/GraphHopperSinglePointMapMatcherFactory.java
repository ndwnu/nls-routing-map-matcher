package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.AbstractMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;

public class GraphHopperSinglePointMapMatcherFactory extends AbstractMapMatcherFactory
        implements SinglePointMapMatcherFactory {

    public GraphHopperSinglePointMapMatcherFactory(NetworkGraphHopperFactory networkGraphHopperFactory) {
        super(networkGraphHopperFactory);
    }

    @Override
    public SinglePointMapMatcher createMapMatcher(RoutingNetwork routingNetwork) {
        return new GraphHopperSinglePointMapMatcher(readNetwork(routingNetwork));
    }

    @Override
    public SinglePointMapMatcher createMapMatcher(Network preInitializedNetwork) {
        return new GraphHopperSinglePointMapMatcher((NetworkGraphHopper) preInitializedNetwork);
    }
}
