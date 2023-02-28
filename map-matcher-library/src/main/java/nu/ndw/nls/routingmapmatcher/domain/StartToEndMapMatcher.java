package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;

/* This interface contains the same method as LineStringMapMatcher, but marks matchers that only use the start and end
 * point of the provided LineString as input, disregarding any waypoints except in the calculation of the path score. */
public interface StartToEndMapMatcher extends MapMatcher<LineStringLocation, LineStringMatch> {

    LineStringMatch match(LineStringLocation lineStringLocation);
}
