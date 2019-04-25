package nl.dat.routingmapmatcher.linestring;

import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.vividsolutions.jts.geom.LineString;

public class LineStringLocation {

  private final int id;
  private final Optional<Integer> locationIndex;
  private final Optional<Boolean> reversed;
  private final double lengthInMeters;
  private final LineString geometry;
  private final ReliabilityCalculationType reliabilityCalculationType;

  public LineStringLocation(final int id, final Optional<Integer> locationIndex, final Optional<Boolean> reversed,
      final double lengthInMeters, final LineString geometry, final ReliabilityCalculationType reliabilityCalculationType) {
    this.id = id;
    this.locationIndex = locationIndex;
    this.reversed = reversed;
    this.lengthInMeters = lengthInMeters;
    this.geometry = geometry;
    this.reliabilityCalculationType = reliabilityCalculationType;
  }

  public int getId() {
    return id;
  }

  public Optional<Integer> getLocationIndex() {
    return locationIndex;
  }

  public Optional<Boolean> getReversed() {
    return reversed;
  }

  public double getLengthInMeters() {
    return lengthInMeters;
  }

  public LineString getGeometry() {
    return geometry;
  }

  public ReliabilityCalculationType getReliabilityCalculationType() {
    return reliabilityCalculationType;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("locationIndex", locationIndex.orElse(null))
        .add("reversed", reversed.orElse(null))
        .add("lengthInMeters", lengthInMeters)
        .toString();
  }

}
