package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class ViterbiLinestringMapMatcherFactory  implements LineStringMapMatcherFactory {

    private static final Logger logger = LoggerFactory.getLogger(ViterbiLinestringMapMatcherFactory.class);
    private final NetworkGraphHopperFactory networkGraphHopperFactory;

    @Override
    public LineStringMapMatcher createLineStringMapMatcher(final RoutingNetwork routingNetwork) {
        return new ViterbiLineStringMapMatcher(readNetwork(routingNetwork));
    }

    private NetworkGraphHopper readNetwork(final RoutingNetwork routingNetwork) {
        logger.info("Start reading network with version {}", routingNetwork.getNetworkNameAndVersion());
        return networkGraphHopperFactory.createNetworkGraphHopper(routingNetwork);
    }
}
