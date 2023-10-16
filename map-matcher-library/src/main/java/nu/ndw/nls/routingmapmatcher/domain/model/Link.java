package nu.ndw.nls.routingmapmatcher.domain.model;

import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.FORWARD_SUFFIX;
import static nu.ndw.nls.routingmapmatcher.domain.model.LinkTag.REVERSE_SUFFIX;

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

    public <T> void setTag(LinkTag<T> tag, T value, boolean reverse) {
        if(!tag.isSeparateValuesPerDirection()) {
            throw new IllegalArgumentException(
                    ("Link tag %s does not store separate values for both directions. "
                     + "Use setTag method without boolean 'reverse' parameter.").formatted(tag.getLabel()));
        }
        String suffix = reverse ? REVERSE_SUFFIX : FORWARD_SUFFIX;
        this.setTag(tag.getLabel() + suffix, value);
    }

    public <T> T getTag(LinkTag<T> tag, T defaultValue, boolean reverse) {
        if(!tag.isSeparateValuesPerDirection()) {
            throw new IllegalArgumentException(
                    ("Link tag %s does not store separate values for both directions. "
                     + "Use getTag method without boolean 'reverse' parameter.").formatted(tag.getLabel()));
        }
        String suffix = reverse ? REVERSE_SUFFIX : FORWARD_SUFFIX;
        return this.getTag(tag.getLabel() + suffix, defaultValue);
    }

    public <T> void setTag(LinkTag<T> tag, T value) {
        if(tag.isSeparateValuesPerDirection()) {
            throw new IllegalArgumentException(
                    ("Link tag %s stores separate values for both directions. "
                     + "Use setTag method with boolean 'reverse' parameter.").formatted(tag.getLabel()));
        }
        this.setTag(tag.getLabel(), value);
    }

    public <T> T getTag(LinkTag<T> tag, T defaultValue) {
        if(tag.isSeparateValuesPerDirection()) {
            throw new IllegalArgumentException(
                    ("Link tag %s stores separate values for both directions. "
                     + "Use getTag method with boolean 'reverse' parameter.").formatted(tag.getLabel()));
        }
        return this.getTag(tag.getLabel(), defaultValue);
    }

}
