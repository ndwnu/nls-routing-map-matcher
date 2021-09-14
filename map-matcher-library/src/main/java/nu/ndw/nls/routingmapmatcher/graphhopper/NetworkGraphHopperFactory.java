package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

import java.util.Arrays;

public class NetworkGraphHopperFactory {

    private static final String DEFAULT_FOLDER_PREFIX = "graphhopper";
    private static final int BYTES_FOR_EDGE_FLAGS = 12;

    public NetworkGraphHopper createNetworkGraphHopper(final RoutingNetwork routingNetwork) {
        return createNetworkGraphHopper(routingNetwork, false, DEFAULT_FOLDER_PREFIX);
    }

    public NetworkGraphHopper createNetworkGraphHopper(final RoutingNetwork routingNetwork, final boolean storeOnDisk,
            final String folderPrefix) {
        final NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());
        graphHopper.setStoreOnFlush(storeOnDisk);
        graphHopper.setElevation(false);
        graphHopper.setCHEnabled(false);
        graphHopper.setMinNetworkSize(0, 0);
        final String networkFolder = folderPrefix + "_" + routingNetwork.getNetworkNameAndVersion();
        graphHopper.setDataReaderFile(networkFolder);
        graphHopper.setGraphHopperLocation(networkFolder);
        final LinkFlagEncoder flagEncoder = new LinkFlagEncoder();
        graphHopper.setEncodingManager(EncodingManager.create(Arrays.asList(flagEncoder), BYTES_FOR_EDGE_FLAGS));
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);

        return graphHopper;
    }
}
