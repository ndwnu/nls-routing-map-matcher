package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public interface LineStringMapMatcherFactory {
    LineStringMapMatcher createLineStringMapMatcher(RoutingNetwork routingNetwork);
}
