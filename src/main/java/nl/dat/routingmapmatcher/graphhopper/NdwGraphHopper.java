package nl.dat.routingmapmatcher.graphhopper;

import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;

import com.graphhopper.GraphHopper;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;

import nl.dat.routingmapmatcher.enums.NdwNetworkSubset;

public class NdwGraphHopper extends GraphHopper {

  private final Handle handle;
  private final NdwNetworkSubset subset;
  private final List<NdwLinkProperties> linkProperties;

  public NdwGraphHopper(final Handle handle, final NdwNetworkSubset subset) {
    this.handle = handle;
    this.subset = subset;
    this.linkProperties = new ArrayList<>();
  }

  @Override
  protected DataReader createReader(final GraphHopperStorage ghStorage) {
    return initDataReader(new NdwNetworkReader(ghStorage, handle, subset, linkProperties));
  }

  /**
   * @return extra properties per NDW link. The way flags of an edge in the graph contain the index
   *         to use.
   */
  public NdwLinkProperties getLinkProperties(final int index) {
    return linkProperties.get(index);
  }

}
