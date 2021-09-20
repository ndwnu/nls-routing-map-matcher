package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

import static java.util.Collections.singletonList;

public class NetworkGraphHopperFactory {

    private static final String DEFAULT_FOLDER_PREFIX = "graphhopper_";
    private static final int BYTES_FOR_EDGE_FLAGS = 12;

    public NetworkGraphHopper createNetwork(final RoutingNetwork routingNetwork) {
        return createNetwork(routingNetwork, false, DEFAULT_FOLDER_PREFIX);
    }

    public NetworkGraphHopper createNetwork(final RoutingNetwork routingNetwork, final boolean storeOnDisk,
            final String folderPrefix) {
        final NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());
        graphHopper.setStoreOnFlush(storeOnDisk);
        graphHopper.setElevation(false);
        graphHopper.setCHEnabled(false);
        graphHopper.setMinNetworkSize(0, 0);
        final String networkFolder = folderPrefix + routingNetwork.getNetworkNameAndVersion();
        graphHopper.setDataReaderFile(networkFolder);
        graphHopper.setGraphHopperLocation(networkFolder);
        final LinkFlagEncoder flagEncoder = new LinkFlagEncoder();
        graphHopper.setEncodingManager(EncodingManager.create(singletonList(flagEncoder), BYTES_FOR_EDGE_FLAGS));
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);

        return graphHopper;
    }
}
