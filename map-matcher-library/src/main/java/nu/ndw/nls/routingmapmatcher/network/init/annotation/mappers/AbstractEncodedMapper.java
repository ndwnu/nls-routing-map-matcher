package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.IntsRef;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;
import nu.ndw.nls.routingmapmatcher.network.model.Link;

/**
 * TagParsers are actually mappers. They are called TagParsers, because GraphHopper stores the values inside a
 * {@link ReaderWay} (our {@link Link}) map. We use annotations and use reflection and we don't use the map. Our data
 * is acquired from the getter methods of our link extending classes.
 *
 * @param <T> Link type from which the field is obtained
 * @param <U> Field type of the value that is obtained
 */
@RequiredArgsConstructor
public abstract class AbstractEncodedMapper<T extends Link, U> implements TagParser {

    private final EncodedValueDto<T, U> encodedValueDto;

    @Override
    public void handleWayTags(int edgeId, EdgeIntAccess edgeIntAccess, ReaderWay way, IntsRef relationFlags) {
        T link = (T) way;

        U forward = encodedValueDto.valueSupplier().apply(link);

        if (forward != null) {
            set(false, edgeId, edgeIntAccess, forward);
        }

        if (encodedValueDto.isDirectional()) {
            U reverse = encodedValueDto.valueReverseSupplier().apply(link);
            if (reverse != null) {
                set(true, edgeId, edgeIntAccess, reverse);
            }
        }
    }

    /**
     * Is called by {@link #handleWayTags} with the value acquired from the link and should encode the value into
     * the network
     *
     * @param reverse true if the value to encode is the reverse value
     * @param edgeId the edge id
     * @param edgeIntAccess the edge int access
     * @param value the value retrieved from the field, that needs to be encoded into the network
     */
    protected abstract void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, U value);
}
