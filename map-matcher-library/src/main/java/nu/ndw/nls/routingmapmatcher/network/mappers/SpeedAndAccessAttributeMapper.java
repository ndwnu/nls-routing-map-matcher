package nu.ndw.nls.routingmapmatcher.network.mappers;

import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import org.springframework.stereotype.Component;

@Component
public class SpeedAndAccessAttributeMapper {

    public String mapToSpeedAttribute(Profile profile) {
        return VehicleSpeed.key(profile.getName());
    }

    public String mapToAccessAttribute(Profile profile) {
        return VehicleAccess.key(profile.getName());
    }
}
