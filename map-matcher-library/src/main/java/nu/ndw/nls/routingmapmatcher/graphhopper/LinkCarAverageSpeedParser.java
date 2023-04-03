package nu.ndw.nls.routingmapmatcher.graphhopper;



import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
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


public class LinkCarAverageSpeedParser extends AbstractAverageSpeedParser {

    public static final double CAR_MAX_SPEED = 130;
    public static final String NAME_PROPERTY = "name";

    public LinkCarAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString(NAME_PROPERTY, VEHICLE_CAR))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString(NAME_PROPERTY, VEHICLE_CAR)))
                        .getNextStorableValue(CAR_MAX_SPEED)
        );
    }

    protected LinkCarAverageSpeedParser(DecimalEncodedValue speedEnc, double maxPossibleSpeed) {
        super(speedEnc, maxPossibleSpeed);
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
