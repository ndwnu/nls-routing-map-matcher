package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.google.common.base.Supplier;
import com.graphhopper.routing.util.EncodingManager;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;

import java.util.Arrays;
import java.util.Iterator;

public class NetworkGraphHopperFactory {

  private static final String DUMMY_STRING = "dummy";
  private static final int BYTES_FOR_EDGE_FLAGS = 12;

  public NetworkGraphHopper createNetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier) {
    final NetworkGraphHopper graphHopper = new NetworkGraphHopper(linkSupplier);
    graphHopper.setStoreOnFlush(false);
    graphHopper.setElevation(false);
    graphHopper.setCHEnabled(false);
    graphHopper.setMinNetworkSize(0, 0);
    graphHopper.setDataReaderFile(DUMMY_STRING);
    graphHopper.setGraphHopperLocation(DUMMY_STRING);
    final LinkFlagEncoder flagEncoder = new LinkFlagEncoder();
    graphHopper.setEncodingManager(EncodingManager.create(Arrays.asList(flagEncoder), BYTES_FOR_EDGE_FLAGS));

    graphHopper.importOrLoad();
    graphHopper.setAllowWrites(false);

    return graphHopper;
  }
}
