package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.WazeDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class WazeRepository {

  private final Jdbi jdbi;

  public WazeRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public List<LineStringLocation> getWazeJams() {
    try (Handle handle = jdbi.open()) {
      final WazeDao wazeDao = handle.attach(WazeDao.class);
      return wazeDao.getWazeJams();
    }
  }

  public void replaceWazeJamsMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final WazeDao wazeDao = handle.attach(WazeDao.class);
      wazeDao.createWazeJamsMatchesTableIfNotExists();
      wazeDao.truncateWazeJamsMatchesTable();
      wazeDao.insertWazeJamsMatches(lineStringMatches);
    });
  }

  public List<LineStringLocation> getWazeIrregularities() {
    try (Handle handle = jdbi.open()) {
      final WazeDao wazeDao = handle.attach(WazeDao.class);
      return wazeDao.getWazeIrregularities();
    }
  }

  public void replaceWazeIrregularitiesMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final WazeDao wazeDao = handle.attach(WazeDao.class);
      wazeDao.createWazeIrregularitiesMatchesTableIfNotExists();
      wazeDao.truncateWazeIrregularitiesMatchesTable();
      wazeDao.insertWazeIrregularitiesMatches(lineStringMatches);
    });
  }

}
