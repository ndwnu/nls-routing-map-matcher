package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public class IntParser implements TagParser {

    private static final String CODE_TOO_LARGE_MSG =
            "Cannot store %s: %d as it is too large (> %d). "
            + "You can disable %s if you do not need to store the municipality codes";
    private final IntEncodedValue intEnc;
    private final LinkTag linkTag;
    private final String key;

    public IntParser(EncodedValueLookup lookup, EncodedTag encodedTag) {
        this.key = encodedTag.getKey();
        this.intEnc = lookup.getIntEncodedValue(key);
        this.linkTag = encodedTag.getLinkTag();
    }

    @Override
    public void handleWayTags(IntsRef edgeFlags, ReaderWay way, IntsRef relationFlags) {
        int municipalityCode = way.getTag(linkTag.getLabel(), 0);
        if (municipalityCode > intEnc.getMaxStorableInt()) {
            throw new IllegalArgumentException(CODE_TOO_LARGE_MSG.formatted(
                    key, municipalityCode, intEnc.getMaxStorableInt(), intEnc.getName()));
        }
        intEnc.setInt(false, edgeFlags, municipalityCode);
    }
}
