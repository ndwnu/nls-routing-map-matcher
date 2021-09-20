package nu.ndw.nls.routingmapmatcher.graphhopper.starttoend;

import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.AbstractMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;

public class GraphHopperStartToEndMapMatcherFactory extends AbstractMapMatcherFactory
        implements MapMatcherFactory<StartToEndMapMatcher> {

    public GraphHopperStartToEndMapMatcherFactory(final NetworkGraphHopperFactory networkGraphHopperFactory) {
        super(networkGraphHopperFactory);
    }

    @Override
    public StartToEndMapMatcher createMapMatcher(final RoutingNetwork routingNetwork) {
        return new GraphHopperStartToEndMapMatcher(readNetwork(routingNetwork));
    }

    @Override
    public StartToEndMapMatcher createMapMatcher(final Network preInitializedNetwork) {
        return new GraphHopperStartToEndMapMatcher((NetworkGraphHopper) preInitializedNetwork);
    }
}
