package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.WAY_ID;

import com.graphhopper.config.Profile;
import com.graphhopper.util.CustomModel;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingProfile;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.CustomVehicleEncodedValuesFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkTagParserFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.LinkVehicleTagParsersFactory;

public final class GraphHopperConfigurator {

    private static final double WEIGHTING_SHORTEST_DISTANCE_INFLUENCE = 10_000D;

    private GraphHopperConfigurator() {
    }

    static void configureGraphHopper(NetworkGraphHopper networkGraphHopper, Path path) {
        networkGraphHopper.setElevation(false);
        networkGraphHopper.setVehicleEncodedValuesFactory(new CustomVehicleEncodedValuesFactory());
        networkGraphHopper.setVehicleTagParserFactory(new LinkVehicleTagParsersFactory());
        networkGraphHopper.setEncodedValueFactory(new CustomEncodedValuesFactory());
        networkGraphHopper.setTagParserFactory(new LinkTagParserFactory());
        networkGraphHopper.setProfiles(new Profile(RoutingProfile.CAR_FASTEST.getLabel())
                        .setVehicle(VehicleType.CAR.getName()),
                new Profile(RoutingProfile.CAR_SHORTEST.getLabel())
                        .setVehicle(VehicleType.CAR.getName())
                        .setCustomModel(new CustomModel().setDistanceInfluence(WEIGHTING_SHORTEST_DISTANCE_INFLUENCE)));
        networkGraphHopper.setEncodedValuesString(WAY_ID.getKey());
        networkGraphHopper.setMinNetworkSize(0);
        networkGraphHopper.setGraphHopperLocation(path.toString());
    }

    static void configureGraphHopperForAccessibility(NetworkGraphHopper networkGraphHopper, Path path) {
        networkGraphHopper.setElevation(false);
        networkGraphHopper.setVehicleEncodedValuesFactory(new CustomVehicleEncodedValuesFactory());
        networkGraphHopper.setVehicleTagParserFactory(new LinkVehicleTagParsersFactory());
        networkGraphHopper.setEncodedValueFactory(new CustomEncodedValuesFactory());
        networkGraphHopper.setTagParserFactory(new LinkTagParserFactory());
        networkGraphHopper.setProfiles(
                new Profile(RoutingProfile.CAR_FASTEST.getLabel())
                        .setVehicle(VehicleType.CAR.getName()),
                new Profile(RoutingProfile.CAR_SHORTEST.getLabel())
                        .setVehicle(VehicleType.CAR.getName())
                        .setCustomModel(new CustomModel().setDistanceInfluence(WEIGHTING_SHORTEST_DISTANCE_INFLUENCE)),
                VehicleType.HGV.createProfile(RoutingProfile.HGV_CUSTOM.getLabel()),
                VehicleType.BUS.createProfile(RoutingProfile.BUS_CUSTOM.getLabel()),
                VehicleType.CAR.createProfile(RoutingProfile.MOTOR_VEHICLE_CUSTOM.getLabel()));

        networkGraphHopper.setEncodedValuesString(
                Stream.of(EncodedTag.values())
                        .map(EncodedTag::getKey)
                        .collect(Collectors.joining(",")));
        networkGraphHopper.setMinNetworkSize(0);
        networkGraphHopper.setGraphHopperLocation(path.toString());
    }
}
