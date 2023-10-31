package nu.ndw.nls.routingmapmatcher.graphhopper.accessibility;

import nu.ndw.nls.routingmapmatcher.domain.AccessibilityMap;
import nu.ndw.nls.routingmapmatcher.domain.AccessibilityMapFactory;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import org.apache.commons.lang3.NotImplementedException;

public class GraphHopperAccessibilityMapFactory implements AccessibilityMapFactory {


    @Override
    public AccessibilityMap createMapMatcher(RoutingNetwork routingNetwork) {
        throw new NotImplementedException();

    }

    @Override
    public AccessibilityMap createMapMatcher(Network preInitializedNetwork) {

        return new GraphHopperAccessibilityMap((NetworkGraphHopper) preInitializedNetwork);
    }
}
