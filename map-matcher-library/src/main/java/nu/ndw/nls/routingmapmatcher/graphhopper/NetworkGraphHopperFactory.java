package nu.ndw.nls.routingmapmatcher.graphhopper;


import com.graphhopper.config.Profile;
import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.util.GraphHopperNetworkPathUtils;


public class NetworkGraphHopperFactory {

    private static final Path DEFAULT_FOLDER_PREFIX = Path.of("graphhopper_");

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
        graphHopper.setVehicleEncodedValuesFactory(new LinkCarVehicleEncodedValuesFactory());
        graphHopper.setVehicleTagParserFactory(new LinkCarVehicleTagParsersFactory());
        graphHopper.setEncodedValueFactory(new LinkWayIdEncodedValuesFactory());
        graphHopper.setProfiles(new Profile("car_fastest")
                .setVehicle("car")
                .setWeighting("fastest"), new Profile("car_shortest")
                .setVehicle("car")
                .setWeighting("shortest"));
        graphHopper.setEncodedValuesString(LinkWayIdEncodedValuesFactory.ID_NAME);
        graphHopper.setMinNetworkSize(0);
        graphHopper.setGraphHopperLocation(path.toString());
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);
        return graphHopper;
    }
}
