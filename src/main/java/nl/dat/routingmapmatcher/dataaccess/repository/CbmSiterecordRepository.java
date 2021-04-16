package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.CbmSiterecordDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class CbmSiterecordRepository implements LineStringLocationRepository {

  private final Jdbi jdbi;

  public CbmSiterecordRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  @Override
  public List<LineStringLocation> getLocations() {
    try (Handle handle = jdbi.open()) {
      final CbmSiterecordDao cbmSiterecordDao = handle.attach(CbmSiterecordDao.class);
      return cbmSiterecordDao.getCbmSiterecords();
    }
  }

  @Override
  public void replaceMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final CbmSiterecordDao cbmSiterecordDao = handle.attach(CbmSiterecordDao.class);
      cbmSiterecordDao.createCbmSiterecordMatchesTableIfNotExists();
      cbmSiterecordDao.truncateCbmSiterecordMatchesTable();
      cbmSiterecordDao.insertCbmSiterecordMatches(lineStringMatches);
    });
  }
}
