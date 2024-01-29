package nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import lombok.Getter;
import nu.ndw.nls.routingmapmatcher.network.model.DirectionalDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;

@Getter
public class LinkAccessParser<T extends Link> implements TagParser {

    private final LinkVehicleMapper<T> linkVehicleMapper;
    private final BooleanEncodedValue accessEnc;

    public LinkAccessParser(EncodedValueLookup lookup, String name, LinkVehicleMapper<T> linkVehicleMapper) {
        this.accessEnc = lookup.getBooleanEncodedValue(VehicleAccess.key(name));
        this.linkVehicleMapper = linkVehicleMapper;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay readerWay, IntsRef intsRef) {
        T link = this.castToLink(readerWay);
        DirectionalDto<Boolean> linkAccessibility = linkVehicleMapper.getAccessibility(link);
        accessEnc.setBool(false, edgeId, edgeIntAccess, linkAccessibility.forward());
        accessEnc.setBool(true, edgeId, edgeIntAccess, linkAccessibility.reverse());
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
