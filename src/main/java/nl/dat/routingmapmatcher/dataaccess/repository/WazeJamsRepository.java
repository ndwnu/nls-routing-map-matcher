package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.WazeJamsDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class WazeJamsRepository implements LineStringLocationRepository {

  private final Jdbi jdbi;

  public WazeJamsRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<LineStringLocation> getLocations() {
    try (Handle handle = jdbi.open()) {
      final WazeJamsDao wazeJamsDao = handle.attach(WazeJamsDao.class);
      return wazeJamsDao.getWazeJams();
    }
  }

  @Override
  public void replaceMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final WazeJamsDao wazeJamsDao = handle.attach(WazeJamsDao.class);
      wazeJamsDao.createWazeJamsMatchesTableIfNotExists();
      wazeJamsDao.truncateWazeJamsMatchesTable();
      wazeJamsDao.insertWazeJamsMatches(lineStringMatches);
    });
  }
}
