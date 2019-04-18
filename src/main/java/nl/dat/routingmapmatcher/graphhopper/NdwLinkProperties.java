package nl.dat.routingmapmatcher.graphhopper;

public class NdwLinkProperties {

  private final int forwardId;
  private final int backwardId;

  public NdwLinkProperties(final int forwardId, final int backwardId) {
    this.forwardId = forwardId;
    this.backwardId = backwardId;
  }

  public int getForwardId() {
    return forwardId;
  }

  public int getBackwardId() {
    return backwardId;
  }

}
