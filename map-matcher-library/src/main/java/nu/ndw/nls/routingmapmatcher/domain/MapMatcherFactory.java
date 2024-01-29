package nu.ndw.nls.routingmapmatcher.domain;


import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;

public interface MapMatcherFactory<T> {

    T createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName);
}
