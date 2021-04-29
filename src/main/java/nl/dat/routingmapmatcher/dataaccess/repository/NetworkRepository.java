package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Query;

import nl.dat.routingmapmatcher.dataaccess.dao.LinkDao;
import nl.dat.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nl.dat.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;

public class NetworkRepository {

  private final Jdbi jdbi;
  private final NetworkGraphHopperFactory networkGraphHopperFactory;

  public NetworkRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
    this.networkGraphHopperFactory = new NetworkGraphHopperFactory();
  }

  public NetworkGraphHopper getNetwork(final String networkQuery) {
    return jdbi.inTransaction((final Handle handle) -> {
      try (final LinkDao linkDao = new LinkDao(handle, networkQuery)) {
        return networkGraphHopperFactory.createNetworkGraphHopper(linkDao);
      }
    });
  }

  public List<Integer> getNodeIds(final String nodeIdsQuery) {
    try (final Handle handle = jdbi.open();
        final Query query = handle.createQuery(nodeIdsQuery)) {
      return query
          .mapTo(Integer.class)
          .list();
    }
  }
}
