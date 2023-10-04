package nu.ndw.nls.routingmapmatcher.graphhopper;


import static nu.ndw.nls.routingmapmatcher.graphhopper.GraphHopperConfigurator.configureGraphHopper;

import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils;


public class NetworkGraphHopperFactory {


    public NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork) {
        return createNetwork(routingNetwork, false, GlobalConstants.DEFAULT_FOLDER_PREFIX);
    }

    public NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork, boolean storeOnDisk,
            Path graphhopperRootPath) {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());
        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());
        graphHopper.setStoreOnFlush(storeOnDisk);
        configureGraphHopper(graphHopper,path);
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);
        return graphHopper;
    }

}
