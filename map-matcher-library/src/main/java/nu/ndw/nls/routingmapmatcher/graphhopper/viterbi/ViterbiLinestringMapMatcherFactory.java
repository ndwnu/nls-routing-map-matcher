package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.AbstractMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;

@Slf4j
public class ViterbiLinestringMapMatcherFactory extends AbstractMapMatcherFactory
        implements MapMatcherFactory<LineStringMapMatcher> {

    public ViterbiLinestringMapMatcherFactory(final NetworkGraphHopperFactory networkGraphHopperFactory) {
        super(networkGraphHopperFactory);
    }

    @Override
    public LineStringMapMatcher createMapMatcher(final RoutingNetwork routingNetwork) {
        return new ViterbiLineStringMapMatcher(readNetwork(routingNetwork));
    }

    @Override
    public LineStringMapMatcher createMapMatcher(final Network preInitializedNetwork) {
        return new ViterbiLineStringMapMatcher((NetworkGraphHopper) preInitializedNetwork);
    }
}
