package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.Arrays;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import com.graphhopper.routing.util.EncodingManager;

import nl.dat.routingmapmatcher.enums.NdwNetworkSubset;
import nl.dat.routingmapmatcher.graphhopper.NdwGraphHopper;
import nl.dat.routingmapmatcher.graphhopper.NdwLinkFlagEncoder;

public class NdwNetworkRepository {

  private static final String DUMMY_STRING = "dummy";
  private static final int BYTES_FOR_EDGE_FLAGS = 8;

  private final Jdbi jdbi;

  public NdwNetworkRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public NdwGraphHopper getNdwNetwork(final NdwNetworkSubset subset) {
    return jdbi.inTransaction((final Handle handle) -> {
      final NdwGraphHopper graphHopper = new NdwGraphHopper(handle, subset);
      graphHopper.setStoreOnFlush(false);
      graphHopper.setElevation(false);
      graphHopper.setCHEnabled(false);
      graphHopper.setMinNetworkSize(0, 0);
      graphHopper.setDataReaderFile(DUMMY_STRING);
      graphHopper.setGraphHopperLocation(DUMMY_STRING);
      final NdwLinkFlagEncoder flagEncoder = new NdwLinkFlagEncoder();
      graphHopper.setEncodingManager(new EncodingManager(Arrays.asList(flagEncoder), BYTES_FOR_EDGE_FLAGS));

      graphHopper.importOrLoad();
      graphHopper.setAllowWrites(false);

      return graphHopper;
    });
  }

}
