package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

public interface MapMatcherFactory<T> {

    T createMapMatcher(final RoutingNetwork routingNetwork);

    T createMapMatcher(final Network preInitializedNetwork);
}
