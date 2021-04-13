package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.WazeIrregularitiesDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class WazeIrregularitiesRepository implements LineStringLocationRepository {

  private final Jdbi jdbi;

  public WazeIrregularitiesRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<LineStringLocation> getLocations() {
    try (Handle handle = jdbi.open()) {
      final WazeIrregularitiesDao wazeIrregularitiesDao = handle.attach(WazeIrregularitiesDao.class);
      return wazeIrregularitiesDao.getWazeIrregularities();
    }
  }

  @Override
  public void replaceMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final WazeIrregularitiesDao wazeIrregularitiesDao = handle.attach(WazeIrregularitiesDao.class);
      wazeIrregularitiesDao.createWazeIrregularitiesMatchesTableIfNotExists();
      wazeIrregularitiesDao.truncateWazeIrregularitiesMatchesTable();
      wazeIrregularitiesDao.insertWazeIrregularitiesMatches(lineStringMatches);
    });
  }
}
