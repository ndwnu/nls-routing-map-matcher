package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;

public final class WayId {

    public static final String KEY = "way_id";
    public static final int TOTAL_BITS_FOR_ENCODING_INTS = 31;
    private WayId() {
    }
    public static IntEncodedValue create() {
        boolean idInTwoDirections = false;
        return new IntEncodedValueImpl(KEY, TOTAL_BITS_FOR_ENCODING_INTS, idInTwoDirections);
    }
}
