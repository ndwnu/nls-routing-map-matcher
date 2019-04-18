package nl.dat.routingmapmatcher.linestring;

import com.google.common.base.MoreObjects;
import com.vividsolutions.jts.geom.LineString;

public class LineStringLocation {

  private final int id;
  private final boolean reversed;
  private final double lengthInMeters;
  private final LineString geometry;

  public LineStringLocation(final int id, final boolean reversed, final double lengthInMeters,
      final LineString geometry) {
    this.id = id;
    this.reversed = reversed;
    this.lengthInMeters = lengthInMeters;
    this.geometry = geometry;
  }

  public int getId() {
    return id;
  }

  public boolean isReversed() {
    return reversed;
  }

  public double getLengthInMeters() {
    return lengthInMeters;
  }

  public LineString getGeometry() {
    return geometry;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("reversed", reversed)
        .add("lengthInMeters", lengthInMeters)
        .toString();
  }

}
