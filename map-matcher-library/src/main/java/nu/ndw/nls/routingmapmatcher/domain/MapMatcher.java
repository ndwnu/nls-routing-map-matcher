package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.base.MapMatch;

public interface MapMatcher<T extends BaseLocation, R extends MapMatch> {

    R match(final T location);
}
