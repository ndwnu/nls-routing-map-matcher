package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.LmsLinkDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class LmsRepository {

  private final Jdbi jdbi;

  public LmsRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public List<LineStringLocation> getLmsLinks() {
    try (Handle handle = jdbi.open()) {
      final LmsLinkDao lmsDao = handle.attach(LmsLinkDao.class);
      return lmsDao.getLmsLinks();
    }
  }

  public void replaceLmsLinkMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final LmsLinkDao lmsDao = handle.attach(LmsLinkDao.class);
      lmsDao.createLmsLinkMatchesTableIfNotExists();
      lmsDao.truncateLmsLinkMatchesTable();
      lmsDao.insertLmsLinkMatches(lineStringMatches);
    });
  }

}
