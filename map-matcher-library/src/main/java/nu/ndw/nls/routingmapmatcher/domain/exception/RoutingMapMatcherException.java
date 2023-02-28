package nu.ndw.nls.routingmapmatcher.domain.exception;

public class RoutingMapMatcherException extends RuntimeException {

    private static final long serialVersionUID = 1898783087894428865L;

    public RoutingMapMatcherException(String message) {
        super(message);
    }

    public RoutingMapMatcherException(Throwable cause) {
        super(cause);
    }

    public RoutingMapMatcherException(String message, Throwable cause) {
        super(message, cause);
    }
}
