package nu.ndw.nls.routingmapmatcher.domain;

import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import org.locationtech.jts.geom.Point;

public interface SinglePointMapMatcher {
    SinglePointMatch match(final Point startPoint);
}
