
package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.util.PMap;

public final class CustomVehicleEncodedValues extends VehicleEncodedValues {


    private static final int DEFAULT_SPEED_BITS = 5;
    private static final String SPEED_BITS = "speed_bits";
    private static final String SPEED_FACTOR = "speed_factor";
    private static final String NAME_PROPERTY = "name";

    private CustomVehicleEncodedValues(String name, BooleanEncodedValue accessEnc, DecimalEncodedValue avgSpeedEnc) {
        super(name, accessEnc, avgSpeedEnc, null, null);
    }

    public static CustomVehicleEncodedValues get(PMap properties, VehicleType vehicleType) {
        String name = properties.getString(NAME_PROPERTY, vehicleType.getName());
        int speedBits = properties.getInt(SPEED_BITS, DEFAULT_SPEED_BITS);
        double speedFactor = properties.getDouble(SPEED_FACTOR, DEFAULT_SPEED_BITS);
        boolean speedTwoDirections = true;
        BooleanEncodedValue accessEnc = VehicleAccess.create(name);
        DecimalEncodedValue averageSpeedEnc = VehicleSpeed.create(name, speedBits, speedFactor, speedTwoDirections);
        return new CustomVehicleEncodedValues(name, accessEnc, averageSpeedEnc);
    }

}
