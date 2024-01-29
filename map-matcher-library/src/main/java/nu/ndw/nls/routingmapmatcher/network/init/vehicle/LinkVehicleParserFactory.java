package nu.ndw.nls.routingmapmatcher.network.init.vehicle;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParserFactory;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.util.PMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAccessParser;
import nu.ndw.nls.routingmapmatcher.network.init.vehicle.parsers.LinkAverageSpeedParser;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.LinkVehicleMapper;

@RequiredArgsConstructor
public class LinkVehicleParserFactory<T extends Link> implements VehicleTagParserFactory {

    private final Map<String, LinkVehicleMapper<T>> linkVehicleMapperMap;

    @Override
    public VehicleTagParsers createParsers(EncodedValueLookup encodedValueLookup, String name, PMap properties) {

        LinkVehicleMapper<T> linkVehicleMapper = linkVehicleMapperMap.get(name);

        LinkAccessParser<T> accessParser = new LinkAccessParser<>(encodedValueLookup, name, linkVehicleMapper);
        LinkAverageSpeedParser<T> averageSpeedParser = new LinkAverageSpeedParser<>(encodedValueLookup, name,
                linkVehicleMapper);

        return new VehicleTagParsers(accessParser, averageSpeedParser, null);
    }

}
