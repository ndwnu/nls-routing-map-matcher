package nl.dat.routingmapmatcher.graphhopper;

import java.util.Arrays;

import com.graphhopper.routing.util.EncodingManager;

import nl.dat.routingmapmatcher.dataaccess.dao.LinkDao;

public class NetworkGraphHopperFactory {

  private static final String DUMMY_STRING = "dummy";
  private static final int BYTES_FOR_EDGE_FLAGS = 12;

  public NetworkGraphHopper createNetworkGraphHopper(final LinkDao linkDao) {
    final NetworkGraphHopper graphHopper = new NetworkGraphHopper(linkDao);
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
