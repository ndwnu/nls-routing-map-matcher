package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class IntParser extends AbstractTagParser<Integer> {

    private static final String CODE_TOO_LARGE_MSG =
            "Cannot store %s: %d as it is too large (> %d). You can disable %s if you do not need it.";
    private final IntEncodedValue intEnc;
    private final String label;

    public IntParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, 0);
        this.intEnc = lookup.getIntEncodedValue(encodedTag.getKey());
        this.label = encodedTag.getLinkTag().getLabel();
    }

    @Override
    protected void set(boolean reverse, IntsRef edgeFlags, Integer value) {
        if (value > intEnc.getMaxStorableInt()) {
            throw new IllegalArgumentException(CODE_TOO_LARGE_MSG.formatted(
                    label, value, intEnc.getMaxStorableInt(), intEnc.getName()));
        }
        intEnc.setInt(reverse, edgeFlags, value);
    }

}
