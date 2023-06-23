package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConfigurator.configureGraphHopper;

import java.nio.file.Path;
import java.util.Objects;
import nu.ndw.nls.routingmapmatcher.domain.exception.GraphHopperNotImportedException;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils;

public class GraphHopperNetworkService {

    /***
     * In order to separate reads and writes to/form disk cache this method only loads existing networks from disk.
     * Use this method to load a previously stored network.
     * @param routingNetwork routing network without link supplier
     * @param graphhopperRootPath the root directory of the graphhopper networks
     * @return a network loaded from disk
     * @throws GraphHopperNotImportedException if there is no network on disk it will throw this exception
     */
    public NetworkGraphHopper loadFromDisk(RoutingNetwork routingNetwork, Path graphhopperRootPath)
            throws GraphHopperNotImportedException {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper();
        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());
        configureGraphHopper(graphHopper, path);
        graphHopper.setAllowWrites(false);
        boolean loaded = graphHopper.load();
        if (!loaded) {
            throw new GraphHopperNotImportedException("The requested graphhopper version is not imported on disk "
                    + routingNetwork.getNetworkNameAndVersion());
        }
        return graphHopper;


    }

    /***
     * In order to separate reads and writes to/form disk cache this method only stores a new network on disk.
     * This method is idempotent it will remove an existing network from disk and reimport it.
     * Use this method to load a previously stored network.
     * @param routingNetwork Routing network with link supplier
     * @param graphhopperRootPath The root directory of the graphhopper networks
     */
    public void storeOnDisk(RoutingNetwork routingNetwork, Path graphhopperRootPath) {
        Objects.requireNonNull(routingNetwork.getLinkSupplier());
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());
        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());
        configureGraphHopper(graphHopper, path);
        graphHopper.clean();
        graphHopper.setStoreOnFlush(true);
        graphHopper.importAndClose();
    }

}
