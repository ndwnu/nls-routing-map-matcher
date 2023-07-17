package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.CAR_FASTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.CAR_SHORTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WEIGHTING_FASTEST;
import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.WEIGHTING_SHORTEST;

import com.graphhopper.config.Profile;
import java.nio.file.Path;

public final class GraphHopperConfigurator {

    private GraphHopperConfigurator() {
    }

    static void configureGraphHopper(NetworkGraphHopper networkGraphHopper, Path path) {
        networkGraphHopper.setElevation(false);
        networkGraphHopper.setVehicleEncodedValuesFactory(new LinkCarVehicleEncodedValuesFactory());
        networkGraphHopper.setVehicleTagParserFactory(new LinkCarVehicleTagParsersFactory());
        networkGraphHopper.setEncodedValueFactory(new LinkWayIdEncodedValuesFactory());
        networkGraphHopper.setProfiles(new Profile(CAR_FASTEST)
                        .setVehicle(VEHICLE_CAR)
                        .setWeighting(WEIGHTING_FASTEST),
                new Profile(CAR_SHORTEST)
                        .setVehicle(VEHICLE_CAR)
                        .setWeighting(WEIGHTING_SHORTEST));
        networkGraphHopper.setEncodedValuesString(LinkWayIdEncodedValuesFactory.ID_NAME);
        networkGraphHopper.setMinNetworkSize(0);
        networkGraphHopper.setGraphHopperLocation(path.toString());
    }
}
