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

public interface FcdDao {

  @SqlQuery("SELECT " +
      "  linkid AS id, " +
      "  NULL AS location_index, " +
      "  false AS reversed, " +
      "  ST_Length(geom::geography) AS length_in_meters, " +
      "  ST_AsEWKB(geom) AS geometry_wkb " +
      "FROM fcd.segments_15342_lvl1 " +
      "ORDER BY linkid")
  @RegisterRowMapper(LineStringLocationMapper.class)
  List<LineStringLocation> getFcdLocations();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.fcd_matches " +
          "( " +
          "  linkid integer NOT NULL PRIMARY KEY, " +
          "  reversed boolean NOT NULL, " +
          "  ndw_link_ids integer[] NOT NULL, " +
          "  start_link_fraction double precision, " +
          "  end_link_fraction double precision, " +
          "  reliability double precision, " +
          "  status text, " +
          "  line_string geography(LineString,4326), " +
          "  CONSTRAINT fcd_matches_fkey_fcd FOREIGN KEY (linkid) " +
          "      REFERENCES fcd.segments_15342_lvl1(linkid) " +
          ") "
  )
  void createFcdMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.fcd_matches")
  void truncateFcdMatchesTable();

  @SqlBatch(
      "INSERT INTO public.fcd_matches(linkid, reversed, ndw_link_ids, " +
          "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
          "  (:id, :reversed, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  int[] insertFcdMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
