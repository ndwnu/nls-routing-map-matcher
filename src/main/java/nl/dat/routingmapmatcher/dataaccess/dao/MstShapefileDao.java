package nl.dat.routingmapmatcher.dataaccess.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import nl.dat.routingmapmatcher.dataaccess.mapper.LineStringLocationMapper;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public interface MstShapefileDao {

  @SqlQuery(
      "SELECT gid AS id, null AS location_index, null AS reversed, lengte AS length_in_meters, ST_AsEWKB(geom) AS geometry_wkb " +
      "FROM public.measurement_site_lines_shapefile " +
      "ORDER BY id ")
  @RegisterRowMapper(LineStringLocationMapper.class)
  List<LineStringLocation> getMstLinesShapefile();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.measurement_site_lines_shapefile_matches " +
      "( " +
      "  gid integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (gid), " +
      "  CONSTRAINT ms_lines_matches_fkey_ms_lines_shapefile FOREIGN KEY (gid) " +
      "      REFERENCES public.measurement_site_lines_shapefile(gid) " +
      ") "
      )
  void createMstLinesShapefileMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.measurement_site_lines_shapefile_matches")
  void truncateMstLinesShapefileMatchesTable();

  @SqlBatch(
      "INSERT INTO public.measurement_site_lines_shapefile_matches(gid, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  void insertMstLinesShapefileMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
