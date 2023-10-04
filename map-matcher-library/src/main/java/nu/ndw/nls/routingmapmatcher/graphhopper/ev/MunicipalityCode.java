package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;

public final class MunicipalityCode {

    public static final String KEY = "municipality_code";
    private static final int BITS = 17;

    private MunicipalityCode() {
    }

    public static IntEncodedValue create() {
        return new IntEncodedValueImpl(KEY, BITS, false);
    }
}
