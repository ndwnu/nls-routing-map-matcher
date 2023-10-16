package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.castToLink;
import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.getAccess;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;


public class CustomAverageSpeedParser extends AbstractAverageSpeedParser {

    private static final String NAME = "name";

    public CustomAverageSpeedParser(EncodedValueLookup lookup, PMap properties, VehicleType vehicleType) {
        super(getDecimalEncodedValue(lookup, properties, vehicleType),
                getDecimalEncodedValue(lookup, properties, vehicleType)
                        .getNextStorableValue(vehicleType.getMaxSpeed()));
    }

    private static DecimalEncodedValue getDecimalEncodedValue(EncodedValueLookup lookup, PMap properties,
            VehicleType vehicleType) {
        return lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString(NAME, vehicleType.getName())));
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay readerWay) {
        WayAccess access = getAccess(readerWay);
        if (access.canSkip()) {
            return;
        }
        Link link = castToLink(readerWay);
        setSpeed(false, edgeFlags, link.getSpeedInKilometersPerHour());
        setSpeed(true, edgeFlags, link.getReverseSpeedInKilometersPerHour());
    }
}
