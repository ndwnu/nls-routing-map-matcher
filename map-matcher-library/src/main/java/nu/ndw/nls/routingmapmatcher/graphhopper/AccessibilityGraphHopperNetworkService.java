package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConfigurator.configureGraphHopperForAccessibility;

import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public class AccessibilityGraphHopperNetworkService extends AbstractGraphHopperNetworkService<NetworkGraphHopper> {

    @Override
    protected void configure(NetworkGraphHopper graphHopper, Path path) {
        configureGraphHopperForAccessibility(graphHopper, path);
    }

    @Override
    protected NetworkGraphHopper createNetwork() {
        return new NetworkGraphHopper();
    }

    @Override
    protected NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork) {
        return new NetworkGraphHopper(routingNetwork.getLinkSupplier());
    }
}
