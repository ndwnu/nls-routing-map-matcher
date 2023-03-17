package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.util.VehicleEncodedValues;
import com.graphhopper.routing.util.VehicleEncodedValuesFactory;
import com.graphhopper.util.PMap;

public class LinkCarVehicleEncodedValuesFactory implements VehicleEncodedValuesFactory {

    @Override
    public VehicleEncodedValues createVehicleEncodedValues(String s, PMap pMap) {
        return CarEncodedValues.car(pMap);
    }


}
