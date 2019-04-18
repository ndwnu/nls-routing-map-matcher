package nl.dat.routingmapmatcher.dataaccess.dto;

import com.google.common.base.Preconditions;
import com.graphhopper.reader.ReaderWay;
import com.vividsolutions.jts.geom.LineString;

public class NdwLinkDto extends ReaderWay {

  private final int forwardId;
  private final int backwardId;
  private final boolean forwardAccess;
  private final boolean backwardAccess;
  private final int fromNodeId;
  private final int toNodeId;
  private final LineString geometry;
  private Integer index;

  public NdwLinkDto(final int forwardId, final int backwardId, final boolean forwardAccess,
      final boolean backwardAccess, final int fromNodeId, final int toNodeId, final LineString geometry) {
    super(Math.max(forwardId, backwardId));
    this.forwardId = forwardId;
    this.backwardId = backwardId;
    this.forwardAccess = forwardAccess;
    this.backwardAccess = backwardAccess;
    this.fromNodeId = fromNodeId;
    this.toNodeId = toNodeId;
    this.geometry = geometry;
  }

  public int getForwardId() {
    return forwardId;
  }

  public int getBackwardId() {
    return backwardId;
  }

  public boolean isForwardAccess() {
    return forwardAccess;
  }

  public boolean isBackwardAccess() {
    return backwardAccess;
  }

  public int getFromNodeId() {
    return fromNodeId;
  }

  public int getToNodeId() {
    return toNodeId;
  }

  public LineString getGeometry() {
    return geometry;
  }

  public int getIndex() {
    Preconditions.checkState(index != null, "make sure to set index before getting index");
    return index;
  }

  public void setIndex(final int index) {
    this.index = index;
  }

}
