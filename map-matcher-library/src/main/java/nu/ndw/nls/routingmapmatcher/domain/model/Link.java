package nu.ndw.nls.routingmapmatcher.domain.model;

import com.graphhopper.reader.ReaderWay;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.LineString;

@Getter
@ToString(exclude = "geometry", callSuper = true)
public final class Link extends ReaderWay {

    private final long fromNodeId;
    private final long toNodeId;
    private final double speedInKilometersPerHour;
    private final double reverseSpeedInKilometersPerHour;
    private final double distanceInMeters;
    private final LineString geometry;

    @Builder
    private Link(long id, long fromNodeId, long toNodeId, double speedInKilometersPerHour,
            double reverseSpeedInKilometersPerHour, double distanceInMeters, LineString geometry) {
        super(id);
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.speedInKilometersPerHour = speedInKilometersPerHour;
        this.reverseSpeedInKilometersPerHour = reverseSpeedInKilometersPerHour;
        this.distanceInMeters = distanceInMeters;
        this.geometry = geometry;
    }

    public void setTag(LinkTag tag, Object value) {
        if(!tag.getClazz().isInstance(value)) {
            throw new IllegalArgumentException("Value for tag \"%s\" should be of class %s, is %s instead"
                    .formatted(tag.getLabel(), tag.getClazz(), value.getClass()));
        }
        this.setTag(tag.getLabel(), value);
    }
}
