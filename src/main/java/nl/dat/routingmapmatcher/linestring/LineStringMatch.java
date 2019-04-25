package nl.dat.routingmapmatcher.linestring;

import java.util.List;
import java.util.Optional;

import com.vividsolutions.jts.geom.LineString;

public class LineStringMatch {

  private final LineStringLocation location;
  private final List<Integer> ndwLinkIds;
  private final double startLinkFraction;
  private final double endLinkFraction;
  private final double reliability;
  private final String status;
  private final LineString lineString;

  public LineStringMatch(final LineStringLocation location, final List<Integer> ndwLinkIds,
      final double startLinkFraction, final double endLinkFraction, final double reliability, final String status,
      final LineString lineString) {
    this.location = location;
    this.ndwLinkIds = ndwLinkIds;
    this.startLinkFraction = startLinkFraction;
    this.endLinkFraction = endLinkFraction;
    this.reliability = reliability;
    this.status = status;
    this.lineString = lineString;
  }

  public LineStringLocation getLocation() {
    return location;
  }

  public List<Integer> getNdwLinkIds() {
    return ndwLinkIds;
  }

  public double getStartLinkFraction() {
    return startLinkFraction;
  }

  public double getEndLinkFraction() {
    return endLinkFraction;
  }

  public double getReliability() {
    return reliability;
  }

  public String getStatus() {
    return status;
  }

  public LineString getLineString() {
    return lineString;
  }

  public int getId() {
    return location.getId();
  }

  public Optional<Integer> getLocationIndex() {
    return location.getLocationIndex();
  }

}
