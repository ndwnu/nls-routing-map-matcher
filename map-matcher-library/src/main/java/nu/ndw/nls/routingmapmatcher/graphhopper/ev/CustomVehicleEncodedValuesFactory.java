package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.routing.util.VehicleEncodedValuesFactory;
import com.graphhopper.util.PMap;

public class CustomVehicleEncodedValuesFactory implements VehicleEncodedValuesFactory {

    @Override
    public VehicleEncodedValues createVehicleEncodedValues(String name, PMap properties) {
        return CustomVehicleEncodedValues.get(properties, VehicleType.ofName(name));
    }

}
