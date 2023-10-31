package nu.ndw.nls.routingmapmatcher.domain;

import java.util.Set;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.AccessibilityRequest;

public interface AccessibilityMap {
    Set<IsochroneMatch> getAccessibleRoadSections (AccessibilityRequest accessibilityRequest);
}
