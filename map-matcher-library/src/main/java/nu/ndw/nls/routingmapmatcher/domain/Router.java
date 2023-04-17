package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingException;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingRequestException;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingResponse;

public interface Router {
    RoutingResponse route(RoutingRequest routingRequest) throws RoutingException, RoutingRequestException;

}
