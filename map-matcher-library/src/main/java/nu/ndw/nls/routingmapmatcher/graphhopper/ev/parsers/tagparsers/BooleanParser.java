package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class BooleanParser extends AbstractTagParser<Boolean> {

    private final BooleanEncodedValue booleanEnc;

    public BooleanParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, true);
        this.booleanEnc = lookup.getBooleanEncodedValue(encodedTag.getKey());
    }

    @Override
    protected void set(boolean reverse, IntsRef edgeFlags, Boolean value) {
        this.booleanEnc.setBool(reverse, edgeFlags, value);
    }
}
