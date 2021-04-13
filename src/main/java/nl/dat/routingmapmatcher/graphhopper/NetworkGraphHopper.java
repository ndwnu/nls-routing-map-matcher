package nl.dat.routingmapmatcher.graphhopper;

import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;

import nl.dat.routingmapmatcher.dataaccess.dao.LinkDao;
import nl.dat.routingmapmatcher.exceptions.RoutingMapMatcherException;

public class NetworkGraphHopper extends GraphHopper {

  private final LinkDao linkDao;
  private final LongIntMap nodeIdToInternalNodeIdMap;

  public NetworkGraphHopper(final LinkDao linkDao) {
    this.linkDao = linkDao;
    this.nodeIdToInternalNodeIdMap = new GHLongIntBTree(200);
  }

  @Override
  protected DataReader createReader(final GraphHopperStorage ghStorage) {
    return initDataReader(new NetworkReader(ghStorage, linkDao, nodeIdToInternalNodeIdMap));
  }

  public int getInternalNodeId(final int nodeId) {
    final int internalNodeId = nodeIdToInternalNodeIdMap.get(nodeId);
    if (internalNodeId < 0) {
      throw new RoutingMapMatcherException("Could not determine internal node id for node id " + nodeId);
    }
    return internalNodeId;
  }
}
