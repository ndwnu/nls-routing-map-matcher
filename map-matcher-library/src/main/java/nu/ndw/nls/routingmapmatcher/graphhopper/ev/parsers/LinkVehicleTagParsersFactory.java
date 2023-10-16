package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParserFactory;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;

public class LinkVehicleTagParsersFactory implements VehicleTagParserFactory {

    @Override
    public VehicleTagParsers createParsers(EncodedValueLookup encodedValueLookup, String name, PMap properties) {
        VehicleType vehicleType = VehicleType.ofName(name);

        return new VehicleTagParsers(new CustomAccessParser(encodedValueLookup, properties, vehicleType),
                new CustomAverageSpeedParser(encodedValueLookup, properties, vehicleType), null);

    }

}
