package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatchWithIsochrone;

public interface SinglePointMapMatcher extends MapMatcher<SinglePointLocation, SinglePointMatch> {
    SinglePointMatchWithIsochrone matchWithIsochrone(
            SinglePointLocation singlePointLocation);
    SinglePointMatch match(SinglePointLocation singlePointLocation);
}
