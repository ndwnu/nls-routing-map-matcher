package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.VehicleTagParserFactory;
import com.graphhopper.routing.util.VehicleTagParsers;
import com.graphhopper.util.PMap;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


public class LinkCarVehicleTagParsersFactory implements VehicleTagParserFactory {

    @Override
    public VehicleTagParsers createParsers(EncodedValueLookup encodedValueLookup, String name, PMap properties) {
        return new VehicleTagParsers(new LinkCarAccessParser(encodedValueLookup, properties),
                new LinkCarAverageSpeedParser(encodedValueLookup, properties), null);
    }

    public static Link castToLink(ReaderWay way) {
        Link link = (Link) way;
        if (link == null) {
            throw new IllegalStateException("Only Link Dto's are supported by this parser");
        }
        return link;
    }
}
