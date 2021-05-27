package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;

public interface LineStringMapMatcher {

  LineStringMatch match(LineStringLocation lineStringLocation);

}
