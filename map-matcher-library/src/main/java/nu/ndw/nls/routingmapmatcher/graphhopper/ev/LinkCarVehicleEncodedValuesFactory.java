package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.routing.util.VehicleEncodedValuesFactory;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.graphhopper.CarEncodedValues;

public class LinkCarVehicleEncodedValuesFactory implements VehicleEncodedValuesFactory {

    @Override
    public VehicleEncodedValues createVehicleEncodedValues(String name, PMap properties) {
        return CarEncodedValues.car(properties);
    }

}
