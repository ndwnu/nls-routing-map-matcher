package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConfigurator.configureGraphHopper;

import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public class GraphHopperNetworkService extends AbstractGraphHopperNetworkService<NetworkGraphHopper> {

    protected void configure(NetworkGraphHopper graphHopper, Path path) {
        configureGraphHopper(graphHopper, path);
    }

    @Override
    protected NetworkGraphHopper createNetwork() {
        return new NetworkGraphHopper();
    }

    @Override
    protected NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork) {
        return new NetworkGraphHopper(routingNetwork.getLinkSupplier(), routingNetwork.getDataDate());
    }
}
