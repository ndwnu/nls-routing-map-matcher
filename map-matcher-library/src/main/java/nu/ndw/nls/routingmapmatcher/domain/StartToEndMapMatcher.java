package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.starttoend.StartToEndMatch;

public interface StartToEndMapMatcher {

    StartToEndMatch match(final StartToEndLocation startToEndLocation);
}
