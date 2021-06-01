package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Slf4j
public class ViterbiLinestringMapMatcherFactory implements LineStringMapMatcherFactory {

    private final NetworkGraphHopperFactory networkGraphHopperFactory;

    @Override
    public LineStringMapMatcher createLineStringMapMatcher(final RoutingNetwork routingNetwork) {
        return new ViterbiLineStringMapMatcher(readNetwork(routingNetwork));
    }

    private NetworkGraphHopper readNetwork(final RoutingNetwork routingNetwork) {
        log.info("Start reading network with version {}", routingNetwork.getNetworkNameAndVersion());
        return networkGraphHopperFactory.createNetworkGraphHopper(routingNetwork);
    }
}
