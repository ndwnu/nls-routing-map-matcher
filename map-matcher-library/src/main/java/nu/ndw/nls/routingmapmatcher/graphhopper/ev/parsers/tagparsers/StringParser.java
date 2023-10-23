package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.StringEncodedValue;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class StringParser extends AbstractTagParser<String> {

    private final StringEncodedValue stringEncodedValue;

    public StringParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, "");
        this.stringEncodedValue = lookup.getStringEncodedValue(encodedTag.getKey());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, String value) {
        stringEncodedValue.setString(reverse, edgeId, edgeIntAccess, value);
    }
}
