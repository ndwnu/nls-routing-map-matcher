package nu.ndw.nls.routingmapmatcher.network.model;

import com.graphhopper.reader.ReaderWay;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import org.locationtech.jts.geom.LineString;

@Getter
@ToString(exclude = "geometry", callSuper = true)
public class Link extends ReaderWay implements NetworkEncoded {

    public static final String WAY_ID_KEY = "way_id";
    public static final String REVERSED_LINK_ID = "reversed_link_id";

    private final long fromNodeId;
    private final long toNodeId;
    private final double distanceInMeters;
    private final LineString geometry;

    @EncodedValue(key = REVERSED_LINK_ID, bits = 31)
    private final long linkIdReversed;

    protected Link(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry) {
        this(id, fromNodeId, toNodeId, distanceInMeters, geometry, null);
    }

    protected Link(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry,
            Long linkIdReversed) {
        super(id);
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distanceInMeters = distanceInMeters;
        this.geometry = geometry;
        this.linkIdReversed = linkIdReversed != null ? linkIdReversed : 0L;
    }

    @Override
    @EncodedValue(key = WAY_ID_KEY, bits = 31)
    public long getId() {
        return super.getId();
    }
}
