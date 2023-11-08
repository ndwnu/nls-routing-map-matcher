package nu.ndw.nls.routingmapmatcher.graphhopper;

import java.nio.file.Path;
import java.util.Objects;
import nu.ndw.nls.routingmapmatcher.domain.exception.GraphHopperNotImportedException;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils;

public abstract class AbstractGraphHopperNetworkService<T extends NetworkGraphHopper> {

    /***
     * In order to separate reads and writes to/form disk cache this method only loads existing networks from disk.
     * Use this method to load a previously stored network.
     * @param routingNetwork routing network without link supplier
     * @param graphhopperRootPath the root directory of the graphhopper networks
     * @return a network loaded from disk
     * @throws GraphHopperNotImportedException if there is no network on disk it will throw this exception
     */
    public T loadFromDisk(RoutingNetwork routingNetwork, Path graphhopperRootPath)
            throws GraphHopperNotImportedException {
        T graphHopper = createNetwork();
        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());
        configure(graphHopper, path);
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
        T graphHopper = createNetwork(routingNetwork);
        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());
        configure(graphHopper, path);
        graphHopper.clean();
        graphHopper.setStoreOnFlush(true);
        graphHopper.importAndClose();
    }

    protected abstract void configure(T graphHopper, Path path);

    protected abstract T createNetwork();

    protected abstract T createNetwork(RoutingNetwork routingNetwork);
}
