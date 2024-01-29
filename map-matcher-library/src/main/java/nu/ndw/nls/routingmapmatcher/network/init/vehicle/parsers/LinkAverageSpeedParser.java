package nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.parsers.AbstractAverageSpeedParser;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;

@Getter
public class LinkAverageSpeedParser<T extends Link> extends AbstractAverageSpeedParser {

    private static final double NEAR_ZERO = 0.1;
    private final LinkVehicleMapper<T> linkVehicleMapper;

    public LinkAverageSpeedParser(EncodedValueLookup lookup, String name, LinkVehicleMapper<T> linkVehicleMapper) {
        super(lookup.getDecimalEncodedValue(VehicleSpeed.key(name)), null);
        this.linkVehicleMapper = linkVehicleMapper;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay) {
        DirectionalDto<Double> linkSpeed = linkVehicleMapper.getSpeed(this.castToLink(readerWay));
        setSpeed(false, edgeId, edgeIntAccess, linkSpeed.forward());
        setSpeed(true, edgeId, edgeIntAccess, linkSpeed.reverse());
    }

    @Override
    protected void setSpeed(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, double speed) {
        if (speed > NEAR_ZERO) {
            double minSpeed = avgSpeedEnc.getSmallestNonZeroValue();
            double maxSpeed = avgSpeedEnc.getMaxStorableDecimal();
            super.setSpeed(reverse, edgeId, edgeIntAccess, Math.min(Math.max(speed, minSpeed), maxSpeed));
        }
    }

    private T castToLink(ReaderWay way) {
        if (linkVehicleMapper.getLinkClass().isInstance(way)) {
            return linkVehicleMapper.getLinkClass().cast(way);
        } else {
            throw new IllegalStateException("Only links of type '%s' are supported by this parser"
                    .formatted(linkVehicleMapper.getLinkClass().getSimpleName()));
        }
    }
}
