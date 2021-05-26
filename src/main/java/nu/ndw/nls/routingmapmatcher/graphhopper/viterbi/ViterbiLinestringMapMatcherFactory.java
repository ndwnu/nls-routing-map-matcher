package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViterbiLinestringMapMatcherFactory  implements LineStringMapMatcherFactory {


    private static final Logger logger = LoggerFactory.getLogger(ViterbiLinestringMapMatcherFactory.class);
    private final NetworkGraphHopperFactory networkGraphHopperFactory;

    public ViterbiLinestringMapMatcherFactory() {
        networkGraphHopperFactory = new NetworkGraphHopperFactory();
    }

    @Override
    public LineStringMapMatcher createLineStringMapMatcher(RoutingNetwork routingNetwork) {
        return new ViterbiLineStringMapMatcher(readNetwork(routingNetwork));
    }

    private NetworkGraphHopper readNetwork(RoutingNetwork routingNetwork) {
        logger.info("Start reading network with version {}", routingNetwork.getNetworkVersion());
        return  networkGraphHopperFactory.createNetworkGraphHopper(routingNetwork);
    }
}
