package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConfigurator.configureGraphHopperForAccessibility;

import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public class IndexedGraphHopperNetworkService extends AbstractGraphHopperNetworkService<IndexedNetworkGraphHopper> {

    @Override
    protected void configure(IndexedNetworkGraphHopper graphHopper, Path path) {
        configureGraphHopperForAccessibility(graphHopper, path);
    }

    @Override
    protected IndexedNetworkGraphHopper createNetwork() {
        return new IndexedNetworkGraphHopper();
    }

    @Override
    protected IndexedNetworkGraphHopper createNetwork(RoutingNetwork routingNetwork) {
        return new IndexedNetworkGraphHopper(routingNetwork.getLinkSupplier());
    }

}
