package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.domain.model.RoutingNetwork;

import java.util.Arrays;

public class NetworkGraphHopperFactory {

  private static final int BYTES_FOR_EDGE_FLAGS = 12;

  public NetworkGraphHopper createNetworkGraphHopper(RoutingNetwork routingNetwork) {
    final NetworkGraphHopper graphHopper = new NetworkGraphHopper(routingNetwork.getLinkSupplier());
    graphHopper.setStoreOnFlush(false);
    graphHopper.setElevation(false);
    graphHopper.setCHEnabled(false);
    graphHopper.setMinNetworkSize(0, 0);
    graphHopper.setDataReaderFile("graphhopper_"+routingNetwork.getNetworkVersion());
    graphHopper.setGraphHopperLocation("graphhopper_"+routingNetwork.getNetworkVersion());
    final LinkFlagEncoder flagEncoder = new LinkFlagEncoder();
    graphHopper.setEncodingManager(EncodingManager.create(Arrays.asList(flagEncoder), BYTES_FOR_EDGE_FLAGS));

    graphHopper.importOrLoad();
    graphHopper.setAllowWrites(false);

    return graphHopper;
  }
}
