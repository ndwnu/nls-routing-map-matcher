package nu.ndw.nls.routingmapmatcher.graphhopper;



import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkCarVehicleTagParsersFactory.castToLink;
import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkCarVehicleTagParsersFactory.getAccess;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.TransportationMode;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


public class LinkCarAccessParser extends AbstractAccessParser implements TagParser {

    public LinkCarAccessParser(EncodedValueLookup lookup, PMap properties) {
        this(lookup.getBooleanEncodedValue(VehicleAccess.key(properties.getString("name", "car"))),
                TransportationMode.CAR);
    }

    protected LinkCarAccessParser(BooleanEncodedValue accessEnc,
            TransportationMode transportationMode) {
        super(accessEnc, transportationMode);
    }


    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay readerWay) {

        WayAccess access = getAccess(readerWay);
        if (access.canSkip()) {
            return;
        }
        Link link = castToLink(readerWay);
        accessEnc.setBool(false, edgeFlags, link.getSpeedInKilometersPerHour() > 0.0);
        accessEnc.setBool(true, edgeFlags, link.getReverseSpeedInKilometersPerHour() > 0.0);

    }
}
