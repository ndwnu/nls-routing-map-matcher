package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.google.common.base.Supplier;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;

import java.util.Iterator;

public class NetworkRepository {


    private final NetworkGraphHopperFactory networkGraphHopperFactory;

    public NetworkRepository() {
        this.networkGraphHopperFactory = new NetworkGraphHopperFactory();
    }

    public NetworkGraphHopper getNetwork(Supplier<Iterator<Link>> linkSupplier) {
        return networkGraphHopperFactory.createNetworkGraphHopper(linkSupplier);

    }

}
