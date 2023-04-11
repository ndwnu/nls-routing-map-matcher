
package nu.ndw.nls.routingmapmatcher.graphhopper;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.util.PMap;

public class CarEncodedValues extends VehicleEncodedValues {


    private static final int DEFAULT_SPEED_BITS = 5;
    private static final String SPEED_BITS = "speed_bits";
    private static final String SPEED_FACTOR = "speed_factor";
    private static final String NAME_PROPERTY = "name";
    private final String name;


    public CarEncodedValues(String name, BooleanEncodedValue accessEnc, DecimalEncodedValue avgSpeedEnc) {
        super(name, accessEnc, avgSpeedEnc, null, null);
        this.name = name;
    }

    public static CarEncodedValues car(PMap properties) {
        String name = properties.getString(NAME_PROPERTY, VEHICLE_CAR);
        int speedBits = properties.getInt(SPEED_BITS, DEFAULT_SPEED_BITS);
        double speedFactor = properties.getDouble(SPEED_FACTOR, DEFAULT_SPEED_BITS);
        boolean speedTwoDirections = true;
        BooleanEncodedValue accessEnc = VehicleAccess.create(name);
        DecimalEncodedValue averageSpeedEnc = VehicleSpeed.create(name, speedBits, speedFactor, speedTwoDirections);
        return new CarEncodedValues(name, accessEnc, averageSpeedEnc);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}