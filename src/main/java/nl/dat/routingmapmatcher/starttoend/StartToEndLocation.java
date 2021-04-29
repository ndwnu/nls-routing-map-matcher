package nl.dat.routingmapmatcher.starttoend;

import org.locationtech.jts.geom.Point;

public class StartToEndLocation {

  private final int id;
  private final int locationIndex;
  private final double lengthAffected;
  private final Point startPoint;
  private final Point endPoint;

  public StartToEndLocation(final int id, final int locationIndex, final double lengthAffected, final Point startPoint,
      final Point endPoint) {
    this.id = id;
    this.locationIndex = locationIndex;
    this.lengthAffected = lengthAffected;
    this.startPoint = startPoint;
    this.endPoint = endPoint;
  }

  public int getId() {
    return id;
  }

  public int getLocationIndex() {
    return locationIndex;
  }

  public double getLengthAffected() {
    return lengthAffected;
  }

  public Point getStartPoint() {
    return startPoint;
  }

  public Point getEndPoint() {
    return endPoint;
  }
}
