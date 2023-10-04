package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class BooleanParser implements TagParser {

    private final BooleanEncodedValue booleanEnc;
    private final LinkTag linkTag;

    public BooleanParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        this.booleanEnc = lookup.getBooleanEncodedValue(encodedTag.getKey());
        this.linkTag = encodedTag.getLinkTag();
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
        boolean booleanValue = way.getTag(linkTag.getLabel(), true);
        booleanEnc.setBool(false, edgeFlags, booleanValue);
    }
}
