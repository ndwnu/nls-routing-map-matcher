package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.StartToEndLocationDao;
import nl.dat.routingmapmatcher.dataaccess.dao.StartToEndMatchDao;
import nl.dat.routingmapmatcher.starttoend.StartToEndLocation;
import nl.dat.routingmapmatcher.starttoend.StartToEndMatch;

public class StartToEndRepository {

  private final Jdbi jdbi;

  public StartToEndRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public List<StartToEndLocation> getNoFcdStartToEndMeasurementLocations() {
    try (Handle handle = jdbi.open()) {
      final StartToEndLocationDao startToEndLocationDao = handle.attach(StartToEndLocationDao.class);
      return startToEndLocationDao.getNoFcdStartToEndMeasurementLocations();
    }
  }

  public void replaceMatches(final List<StartToEndMatch> startToEndMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final StartToEndMatchDao startToEndMatchDao = handle.attach(StartToEndMatchDao.class);
      startToEndMatchDao.createMeasurementSiteLocationMatchesTableIfNotExists();
      startToEndMatchDao.truncateMeasurementSiteLocationMatchesTable();
      startToEndMatchDao.insertMeasurementSiteLocationMatches(startToEndMatches);
    });
  }

}
