package nu.ndw.nls.routingmapmatcher.graphhopper;

class AccessibilityGraphHopperNetworkServiceIT extends AbstractGraphHopperNetworkServiceIT {

    @Override
    AbstractGraphHopperNetworkService<? extends NetworkGraphHopper> createGraphHopperNetworkService() {
        return new AccessibilityGraphHopperNetworkService();
    }
}
