package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocationWithBearing;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;

public interface SinglePointMapMatcher extends MapMatcher<SinglePointLocation, SinglePointMatch> {

    SinglePointMatch match(final SinglePointLocation singlePointLocation);

    SinglePointMatch matchWithBearing(final SinglePointLocationWithBearing singlePointLocationWithBearing);
}
