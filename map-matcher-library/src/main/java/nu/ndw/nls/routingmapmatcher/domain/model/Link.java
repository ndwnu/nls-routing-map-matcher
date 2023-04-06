package nu.ndw.nls.routingmapmatcher.domain.model;

import com.graphhopper.reader.ReaderWay;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.LineString;

@Getter
@ToString(exclude = "geometry", callSuper = true)
public class Link extends ReaderWay {

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
}
