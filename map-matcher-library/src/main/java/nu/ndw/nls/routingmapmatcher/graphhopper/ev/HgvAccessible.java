package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;

public final class HgvAccessible {

    public static final String KEY = "hgv_accessible";

    private HgvAccessible() {
    }

    public static BooleanEncodedValue create() {
        return new SimpleBooleanEncodedValue(KEY, false);
    }

}
