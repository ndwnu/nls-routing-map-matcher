package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueFactory;
import com.graphhopper.routing.ev.IntEncodedValueImpl;
import com.graphhopper.util.PMap;

public class LinkWayIdEncodedValuesFactory implements EncodedValueFactory {
    public static final String ID_NAME = "way_id";
    private static final int TOTAL_BITS_FOR_ENCODING_INTS = 31;

    @Override
    public EncodedValue create(String name, PMap properties) {
        boolean idInTwoDirections = false;
        return new IntEncodedValueImpl(name, TOTAL_BITS_FOR_ENCODING_INTS, idInTwoDirections);
    }

}
