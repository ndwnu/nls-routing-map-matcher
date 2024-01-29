package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.model.base.MapMatch;

public interface MapMatcher<T extends BaseLocation, R extends MapMatch> {

    R match(T location);
}
