package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParserFactory;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.util.PMap;


public class LinkCarVehicleTagParsersFactory implements VehicleTagParserFactory {

    @Override
    public VehicleTagParsers createParsers(EncodedValueLookup encodedValueLookup, String name, PMap properties) {
        return new VehicleTagParsers(new LinkCarAccessParser(encodedValueLookup, properties),
                new LinkCarAverageSpeedParser(encodedValueLookup, properties), null);
    }

}
