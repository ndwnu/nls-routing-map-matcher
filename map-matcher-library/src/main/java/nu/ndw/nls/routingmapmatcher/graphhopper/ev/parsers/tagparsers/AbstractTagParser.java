package nu.ndw.nls.routingmapmatcher.graphhopper.ev.parsers.tagparsers;

import static nu.ndw.nls.routingmapmatcher.graphhopper.NetworkReader.castToLink;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag;

public abstract class AbstractTagParser<T> implements TagParser {

    private final boolean separateValuesPerDirection;
    private final T defaultValue;
    private final LinkTag<T> linkTag;

    public AbstractTagParser(EncodedTag encodedTag, T defaultValue) {
        this.linkTag = (LinkTag<T>) encodedTag.getLinkTag();
        this.separateValuesPerDirection = encodedTag.isSeparateValuesPerDirection();
        this.defaultValue = defaultValue;
    }

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        Link link = castToLink(way);
        if (separateValuesPerDirection) {
            set(false, edgeId, edgeIntAccess, link.getTag(linkTag, this.defaultValue, false));
            set(true, edgeId, edgeIntAccess, link.getTag(linkTag, this.defaultValue, true));
        } else {
            set(false, edgeId, edgeIntAccess, link.getTag(linkTag, this.defaultValue));
        }
    }

    protected abstract void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, T value);
}
