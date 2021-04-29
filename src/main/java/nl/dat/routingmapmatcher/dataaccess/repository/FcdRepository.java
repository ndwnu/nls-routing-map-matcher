package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.FcdDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class FcdRepository implements LineStringLocationRepository {

  private final Jdbi jdbi;

  public FcdRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<LineStringLocation> getLocations() {
    try (Handle handle = jdbi.open()) {
      final FcdDao fcdDao = handle.attach(FcdDao.class);
      return fcdDao.getFcdLocations();
    }
  }

  @Override
  public void replaceMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final FcdDao fcdDao = handle.attach(FcdDao.class);
      fcdDao.createFcdMatchesTableIfNotExists();
      fcdDao.truncateFcdMatchesTable();
      fcdDao.insertFcdMatches(lineStringMatches);
    });
  }
}
