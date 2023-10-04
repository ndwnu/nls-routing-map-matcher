package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class DoubleParser implements TagParser {

    private final DecimalEncodedValue doubleEnc;
    private final LinkTag linkTag;

    public DoubleParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        this.linkTag = encodedTag.getLinkTag();
        this.doubleEnc = lookup.getDecimalEncodedValue(encodedTag.getKey());
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
        doubleEnc.setDecimal(false, edgeFlags, way.getTag(linkTag.getLabel(), Double.POSITIVE_INFINITY));
    }
}
