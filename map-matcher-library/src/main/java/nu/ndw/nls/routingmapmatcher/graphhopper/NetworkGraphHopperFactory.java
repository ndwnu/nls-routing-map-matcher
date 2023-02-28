package nu.ndw.nls.routingmapmatcher.graphhopper;

import static java.util.Collections.singletonList;

import com.graphhopper.routing.util.EncodingManager;
import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils;

public class NetworkGraphHopperFactory {

    private static final Path DEFAULT_FOLDER_PREFIX = Path.of("graphhopper_");
    private static final int BYTES_FOR_EDGE_FLAGS = 12;

    public NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork) {
        return createNetwork(routingNetwork, false, DEFAULT_FOLDER_PREFIX);
    }

    public NetworkGraphHopper createNetwork(RoutingNetwork routingNetwork, boolean storeOnDisk,
            Path graphhopperRootPath) {
        NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());

        Path path = GraphHopperNetworkPathUtils.formatNormalizedPath(graphhopperRootPath,
                routingNetwork.getNetworkNameAndVersion());

        graphHopper.setStoreOnFlush(storeOnDisk);
        graphHopper.setElevation(false);
        graphHopper.setCHEnabled(false);
        graphHopper.setMinNetworkSize(0, 0);
        graphHopper.setDataReaderFile(path.toString());
        graphHopper.setGraphHopperLocation(path.toString());
        LinkFlagEncoder flagEncoder = new LinkFlagEncoder();
        graphHopper.setEncodingManager(EncodingManager.create(singletonList(flagEncoder), BYTES_FOR_EDGE_FLAGS));
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);

        return graphHopper;
    }
}
