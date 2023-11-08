package nu.ndw.nls.routingmapmatcher.graphhopper;

class GraphHopperNetworkServiceIT extends AbstractGraphHopperNetworkServiceIT {

    @Override
    AbstractGraphHopperNetworkService<? extends NetworkGraphHopper> createGraphHopperNetworkService() {
        return new GraphHopperNetworkService();
    }
}
