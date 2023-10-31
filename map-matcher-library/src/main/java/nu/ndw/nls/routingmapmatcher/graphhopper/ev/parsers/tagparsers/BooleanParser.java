package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class BooleanParser extends AbstractTagParser<Boolean> {

    private final BooleanEncodedValue booleanEnc;

    public BooleanParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, false);
        this.booleanEnc = lookup.getBooleanEncodedValue(encodedTag.getKey());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Boolean value) {
        this.booleanEnc.setBool(reverse, edgeId, edgeIntAccess, value);
    }
}
