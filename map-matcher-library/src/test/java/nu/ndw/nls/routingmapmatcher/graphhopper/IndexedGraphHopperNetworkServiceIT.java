package nu.ndw.nls.routingmapmatcher.graphhopper;

class IndexedGraphHopperNetworkServiceIT extends AbstractGraphHopperNetworkServiceIT {

    @Override
    AbstractGraphHopperNetworkService<? extends NetworkGraphHopper> createGraphHopperNetworkService() {
        return new IndexedGraphHopperNetworkService();
    }
}
