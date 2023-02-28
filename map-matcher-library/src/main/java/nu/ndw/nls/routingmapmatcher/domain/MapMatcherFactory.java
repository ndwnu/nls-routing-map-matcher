package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public interface MapMatcherFactory<T> {

    T createMapMatcher(RoutingNetwork routingNetwork);

    T createMapMatcher(Network preInitializedNetwork);
}
