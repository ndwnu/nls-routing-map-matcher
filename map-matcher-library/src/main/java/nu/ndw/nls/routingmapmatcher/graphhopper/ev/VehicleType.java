package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.util.CustomModel;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;

@RequiredArgsConstructor
@Getter
public enum VehicleType {

    CAR("car", TransportationMode.CAR, null, 130),
    HGV("hgv", TransportationMode.HGV, LinkTag.HGV_ACCESSIBLE, 80),
    BUS("bus", TransportationMode.BUS, LinkTag.BUS_ACCESSIBLE, 100);

    private static final String NONEXISTENT_VEHICLE_MSG = "No vehicle exists with name \"%s\".";

    private final String name;
    private final TransportationMode transportationMode;
    private final LinkTag<Boolean> accessTag;
    private final double maxSpeed;

    public static VehicleType ofName(String vehicleName) {
        return Stream.of(VehicleType.values())
                .filter(value -> value.getName().equals(vehicleName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(NONEXISTENT_VEHICLE_MSG.formatted(vehicleName)));
    }

    public Profile createProfile(String profileName) {
        return new Profile(profileName).setCustomModel(new CustomModel()).setVehicle(this.name);
    }
}
