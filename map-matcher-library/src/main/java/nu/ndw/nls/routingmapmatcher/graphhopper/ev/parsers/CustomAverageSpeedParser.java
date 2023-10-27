package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.castToLink;
import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.getAccess;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.FerrySpeed;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.WayAccess;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;

public class CustomAverageSpeedParser extends AbstractAverageSpeedParser {

    private static final String NAME = "name";
    private static final double NEAR_ZERO = 0.1;

    public CustomAverageSpeedParser(EncodedValueLookup lookup, PMap properties, VehicleType vehicleType) {
        super(lookup.getDecimalEncodedValue(VehicleSpeed.key(properties.getString(NAME, vehicleType.getName()))),
                lookup.getDecimalEncodedValue(FerrySpeed.KEY));
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay) {
        WayAccess access = getAccess(readerWay);
        if (access.canSkip()) {
            return;
        }
        Link link = castToLink(readerWay);
        setSpeed(false, edgeId, edgeIntAccess, link.getSpeedInKilometersPerHour());
        setSpeed(true, edgeId, edgeIntAccess, link.getReverseSpeedInKilometersPerHour());
    }

    @Override
    protected void setSpeed(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, double speed) {
        if (speed > NEAR_ZERO) {
            super.setSpeed(reverse, edgeId, edgeIntAccess, Math.max(speed, avgSpeedEnc.getSmallestNonZeroValue()));
        }
    }
}
