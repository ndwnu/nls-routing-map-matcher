package nl.dat.routingmapmatcher.exceptions;

public class RoutingMapMatcherException extends RuntimeException {

  private static final long serialVersionUID = 1898783087894428865L;

  public RoutingMapMatcherException(final String message) {
    super(message);
  }

  public RoutingMapMatcherException(final Throwable cause) {
    super(cause);
  }

  public RoutingMapMatcherException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
