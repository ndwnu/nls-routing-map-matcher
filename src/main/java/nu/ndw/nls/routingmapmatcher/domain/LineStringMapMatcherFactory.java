package nu.ndw.nls.routingmapmatcher.domain;

import com.google.common.base.Supplier;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

import java.util.Iterator;

public interface LineStringMapMatcherFactory {
    LineStringMapMatcher createLineStringMapMatcher(RoutingNetwork routingNetwork);
}
