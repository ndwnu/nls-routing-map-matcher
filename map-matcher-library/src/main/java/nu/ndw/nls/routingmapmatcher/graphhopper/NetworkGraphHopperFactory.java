package nu.ndw.nls.routingmapmatcher.graphhopper;


import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.CAR_SHORTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WEIGHTING_FASTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WEIGHTING_SHORTEST;

import com.graphhopper.config.Profile;
import java.nio.file.Path;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
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
        graphHopper.setProfiles(new Profile(CAR_FASTEST)
                        .setVehicle(VEHICLE_CAR)
                        .setWeighting(WEIGHTING_FASTEST),
                new Profile(CAR_SHORTEST)
                        .setVehicle(VEHICLE_CAR)
                        .setWeighting(WEIGHTING_SHORTEST));
        graphHopper.setEncodedValuesString(LinkWayIdEncodedValuesFactory.ID_NAME);
        graphHopper.setMinNetworkSize(0);
        graphHopper.setGraphHopperLocation(path.toString());
        graphHopper.importOrLoad();
        graphHopper.setAllowWrites(false);
        return graphHopper;
    }
}
