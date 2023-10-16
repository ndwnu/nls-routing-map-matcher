package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class StringParser extends AbstractTagParser<String> {

    private final StringEncodedValue stringEncodedValue;

    public StringParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, "");
        this.stringEncodedValue = lookup.getStringEncodedValue(encodedTag.getKey());
    }

    @Override
    protected void set(boolean reverse, IntsRef edgeFlags, String value) {
        stringEncodedValue.setString(reverse, edgeFlags, value);
    }
}
