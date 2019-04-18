package nl.dat.routingmapmatcher.dataaccess.repository;

import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import nl.dat.routingmapmatcher.dataaccess.dao.MstShapefileDao;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public class MstShapefileRepository {

  private final Jdbi jdbi;

  public MstShapefileRepository(final Jdbi jdbi) {
    this.jdbi = jdbi;
  }

  public List<LineStringLocation> getMstLinesShapefile() {
    try (Handle handle = jdbi.open()) {
      final MstShapefileDao mstShapefileDao = handle.attach(MstShapefileDao.class);
      return mstShapefileDao.getMstLinesShapefile();
    }
  }

  public void replaceMstLinesShapefileMatches(final List<LineStringMatch> lineStringMatches) {
    jdbi.useTransaction((final Handle handle) -> {
      final MstShapefileDao mstShapefileDao = handle.attach(MstShapefileDao.class);
      mstShapefileDao.createMstLinesShapefileMatchesTableIfNotExists();
      mstShapefileDao.truncateMstLinesShapefileMatchesTable();
      mstShapefileDao.insertMstLinesShapefileMatches(lineStringMatches);
    });
  }

}
