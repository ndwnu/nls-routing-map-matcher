package nl.dat.routingmapmatcher.starttoend;

import java.util.List;

import org.locationtech.jts.geom.LineString;

import nl.dat.routingmapmatcher.enums.MatchStatus;

public class StartToEndMatch {

  private final int id;
  private final int locationIndex;
  private final List<Integer> ndwLinkIds;
  private final double startLinkFraction;
  private final double endLinkFraction;
  private final double reliability;
  private final MatchStatus status;
  private final LineString lineString;

  public StartToEndMatch(final int id, final int locationIndex, final List<Integer> ndwLinkIds,
      final double startLinkFraction, final double endLinkFraction, final double reliability, final MatchStatus status,
      final LineString lineString) {
    this.id = id;
    this.locationIndex = locationIndex;
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

  public int getLocationIndex() {
    return locationIndex;
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

  public MatchStatus getStatus() {
    return status;
  }

  public LineString getLineString() {
    return lineString;
  }
}
