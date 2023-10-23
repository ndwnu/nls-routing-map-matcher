package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

@Slf4j
public class DoubleParser extends AbstractTagParser<Double> {

    private final DecimalEncodedValue doubleEnc;

    public DoubleParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        super(encodedTag, Double.POSITIVE_INFINITY);
        this.doubleEnc = lookup.getDecimalEncodedValue(encodedTag.getKey());
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Double value) {
        doubleEnc.setDecimal(reverse, edgeId, edgeIntAccess, value);
    }
}
