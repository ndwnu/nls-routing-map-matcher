package nu.ndw.nls.routingmapmatcher.graphhopper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

@RequiredArgsConstructor
@Slf4j
public abstract class AbstractMapMatcherFactory {

    private final NetworkGraphHopperFactory networkGraphHopperFactory;

    protected NetworkGraphHopper readNetwork(final RoutingNetwork routingNetwork) {
        log.info("Start reading network with version {}", routingNetwork.getNetworkNameAndVersion());
        return networkGraphHopperFactory.createNetwork(routingNetwork);
    }
}
