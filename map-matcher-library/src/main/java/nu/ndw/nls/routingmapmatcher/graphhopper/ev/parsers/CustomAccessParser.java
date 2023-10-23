package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.castToLink;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.parsers.AbstractAccessParser;
import com.graphhopper.util.PMap;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;

public class CustomAccessParser extends AbstractAccessParser {

    private static final String NAME_PROPERTY = "name";
    private final LinkTag<Boolean> accessTag;

    public CustomAccessParser(EncodedValueLookup lookup, PMap properties, VehicleType vehicleType) {
        super(lookup.getBooleanEncodedValue(
                        VehicleAccess.key(properties.getString(NAME_PROPERTY, vehicleType.getName()))),
                vehicleType.getTransportationMode());
        this.accessTag = vehicleType.getAccessTag();
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay) {
        Link link = castToLink(readerWay);
        accessEnc.setBool(false, edgeId, edgeIntAccess, isForwardAccessible(link));
        accessEnc.setBool(true, edgeId, edgeIntAccess, isReverseAccessible(link));
    }

    protected boolean isForwardAccessible(Link link) {
        return isBooleanAccessible(link, false) && link.getSpeedInKilometersPerHour() > 0.0;
    }

    protected boolean isReverseAccessible(Link link) {
        return isBooleanAccessible(link, true) && link.getReverseSpeedInKilometersPerHour() > 0.0;
    }

    private Boolean isBooleanAccessible(Link link, boolean reverse) {
        return Optional.ofNullable(accessTag)
                .map(tag -> link.getTag(tag, true, reverse))
                .orElse(true);
    }
}
