package nl.dat.routingmapmatcher.linestring;

import java.util.List;

import com.vividsolutions.jts.geom.LineString;

public class LineStringMatch {

  private final int id;
  private final boolean reversed;
  private final List<Integer> ndwLinkIds;
  private final double startLinkFraction;
  private final double endLinkFraction;
  private final double reliability;
  private final String status;
  private final LineString lineString;

  public LineStringMatch(final int id, final boolean reversed, final List<Integer> ndwLinkIds,
      final double startLinkFraction, final double endLinkFraction, final double reliability, final String status,
      final LineString lineString) {
    this.id = id;
    this.reversed = reversed;
    this.ndwLinkIds = ndwLinkIds;
    this.startLinkFraction = startLinkFraction;
    this.endLinkFraction = endLinkFraction;
    this.reliability = reliability;
    this.status = status;
    this.lineString = lineString;
  }

  public int getId() {
    return id;
  }

  public boolean isReversed() {
    return reversed;
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

}
