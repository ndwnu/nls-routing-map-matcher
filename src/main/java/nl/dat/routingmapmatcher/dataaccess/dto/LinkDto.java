package nl.dat.routingmapmatcher.dataaccess.dto;

import org.locationtech.jts.geom.LineString;

import com.graphhopper.reader.ReaderWay;

public class LinkDto extends ReaderWay {

  private final long fromNodeId;
  private final long toNodeId;
  private final double speedInKilometersPerHour;
  private final double reverseSpeedInKilometersPerHour;
  private final double distanceInMeters;
  private final LineString geometry;

  public LinkDto(final long id, final long fromNodeId, final long toNodeId, final double speedInKilometersPerHour,
      final double reverseSpeedInKilometersPerHour, final double distanceInMeters, final LineString geometry) {
    super(id);
    this.fromNodeId = fromNodeId;
    this.toNodeId = toNodeId;
    this.speedInKilometersPerHour = speedInKilometersPerHour;
    this.reverseSpeedInKilometersPerHour = reverseSpeedInKilometersPerHour;
    this.distanceInMeters = distanceInMeters;
    this.geometry = geometry;
  }

  public long getFromNodeId() {
    return fromNodeId;
  }

  public long getToNodeId() {
    return toNodeId;
  }

  public double getSpeedInKilometersPerHour() {
    return speedInKilometersPerHour;
  }

  public double getReverseSpeedInKilometersPerHour() {
    return reverseSpeedInKilometersPerHour;
  }

  public double getDistanceInMeters() {
    return distanceInMeters;
  }

  public LineString getGeometry() {
    return geometry;
  }
}
