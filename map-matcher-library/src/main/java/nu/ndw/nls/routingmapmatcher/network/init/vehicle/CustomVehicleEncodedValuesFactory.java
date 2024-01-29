package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.routing.util.VehicleEncodedValuesFactory;
import com.graphhopper.util.PMap;
import org.springframework.stereotype.Component;

@Component
public class CustomVehicleEncodedValuesFactory implements VehicleEncodedValuesFactory {

    private static final int DEFAULT_SPEED_BITS = 5;

    @Override
    public VehicleEncodedValues createVehicleEncodedValues(String name, PMap properties) {
        BooleanEncodedValue accessEnc = VehicleAccess.create(name);
        DecimalEncodedValue averageSpeedEnc = VehicleSpeed.create(name, DEFAULT_SPEED_BITS, DEFAULT_SPEED_BITS, true);
        return new VehicleEncodedValues(name, accessEnc, averageSpeedEnc, null, null);
    }

}
