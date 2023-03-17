package nu.ndw.nls.routingmapmatcher.graphhopper;


import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkCarVehicleTagParsersFactory.castToLink;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


public class LinkCarAverageSpeedParser extends AbstractAverageSpeedParser implements TagParser {
    public static final double CAR_MAX_SPEED = 140;

    public LinkCarAverageSpeedParser(EncodedValueLookup lookup, PMap properties) {
        this(
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "car"))),
                lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString("name", "car"))).getNextStorableValue(CAR_MAX_SPEED)
        );
    }

    protected LinkCarAverageSpeedParser(DecimalEncodedValue speedEnc, double maxPossibleSpeed) {
        super(speedEnc, maxPossibleSpeed);
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay readerWay) {
        Link link = castToLink(readerWay);
        setSpeed(false, edgeFlags, link.getSpeedInKilometersPerHour());
        setSpeed(true, edgeFlags, link.getReverseSpeedInKilometersPerHour());
    }
}
