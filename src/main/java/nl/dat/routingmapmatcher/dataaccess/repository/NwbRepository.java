package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.NwbDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class NwbRepository {

  private final Jdbi jdbi;

  public NwbRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public List<LineStringLocation> getNwbLocations() {
    try (Handle handle = jdbi.open()) {
      final NwbDao nwbDao = handle.attach(NwbDao.class);
      return nwbDao.getNwbLocations();
    }
  }

  public List<LineStringLocation> getNwbLocationsOnlyNationalHighways() {
    try (Handle handle = jdbi.open()) {
      final NwbDao nwbDao = handle.attach(NwbDao.class);
      return nwbDao.getNwbLocationsOnlyNationalHighways();
    }
  }

  public void replaceNwbMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final NwbDao nwbDao = handle.attach(NwbDao.class);
      nwbDao.createNwbMatchesTableIfNotExists();
      nwbDao.truncateNwbMatchesTable();
      nwbDao.insertNwbMatches(lineStringMatches);
    });
  }

}
