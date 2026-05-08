package nu.ndw.nls.routingmapmatcher.exception;

import java.io.Serial;

public class RoutingMapMatcherException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1898783087894428865L;

    public RoutingMapMatcherException(String message) {
        super(message);
    }

}
