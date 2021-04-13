package nl.dat.routingmapmatcher.dataaccess.dto;

import org.locationtech.jts.geom.Point;

public class SituationRecordLocationDto {

  private final int situationRecordId;
  private final int locationIndex;
  private final String locationType;
  private final Point locationForDisplay;
  private final Point startPoint;
  private final Point endPoint;

  public SituationRecordLocationDto(final int situationRecordId, final int locationIndex, final Point locationForDisplay) {
    this.situationRecordId = situationRecordId;
    this.locationIndex = locationIndex;
    this.locationType = "point";
    this.locationForDisplay = locationForDisplay;
    this.startPoint = null;
    this.endPoint = null;
  }

  public SituationRecordLocationDto(final int situationRecordId, final int locationIndex, final Point startPoint,
      final Point endPoint) {
    this.situationRecordId = situationRecordId;
    this.locationIndex = locationIndex;
    this.locationType = "linear";
    this.locationForDisplay = null;
    this.startPoint = startPoint;
    this.endPoint = endPoint;
  }

  public int getSituationRecordId() {
    return situationRecordId;
  }

  public int getLocationIndex() {
    return locationIndex;
  }

  public String getLocationType() {
    return locationType;
  }

  public Point getLocationForDisplay() {
    return locationForDisplay;
  }

  public Point getStartPoint() {
    return startPoint;
  }

  public Point getEndPoint() {
    return endPoint;
  }
}
