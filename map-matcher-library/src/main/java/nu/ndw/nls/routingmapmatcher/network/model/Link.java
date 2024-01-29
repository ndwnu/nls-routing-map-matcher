package nu.ndw.nls.routingmapmatcher.network.model;

import com.graphhopper.reader.ReaderWay;
import lombok.Getter;
import lombok.ToString;
import nu.ndw.nls.routingmapmatcher.network.annotations.EncodedValue;
import org.locationtech.jts.geom.LineString;

@Getter
@ToString(exclude = "geometry", callSuper = true)
public class Link extends ReaderWay {

    public static final String WAY_ID_KEY = "way_id";

    private final long fromNodeId;
    private final long toNodeId;
    private final double distanceInMeters;
    private final LineString geometry;

    protected Link(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry) {
        super(id);
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.distanceInMeters = distanceInMeters;
        this.geometry = geometry;
    }

    @Override
    @EncodedValue(key = WAY_ID_KEY, bits = 31)
    public long getId() {
        return super.getId();
    }

}
