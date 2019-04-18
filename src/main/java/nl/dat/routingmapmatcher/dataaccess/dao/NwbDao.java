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

public interface NwbDao {

  @SqlQuery(
      "WITH nwb_per_direction AS ( " +
      "  SELECT gid, wegbehsrt, false AS reversed, geom " +
      "  FROM public.nwb " +
      "  WHERE rijrichtng IS NULL OR rijrichtng = 'H' " +
      "  UNION ALL " +
      "  SELECT gid, wegbehsrt, true AS reversed, ST_Reverse(geom) AS geom " +
      "  FROM public.nwb " +
      "  WHERE rijrichtng IS NULL OR rijrichtng = 'T' " +
      ") " +
      "SELECT " +
      "  gid AS id, " +
      "  reversed, " +
      "  ST_Length(ST_Transform(geom, 4326)::geography) AS length_in_meters, " +
      "  ST_AsEWKB(ST_Transform(geom, 4326)) AS geometry_wkb " +
      "FROM nwb_per_direction " +
      "ORDER BY gid, geom")
  @RegisterRowMapper(LineStringLocationMapper.class)
  public List<LineStringLocation> getNwbLocations();

  @SqlQuery(
      "WITH nwb_per_direction AS ( " +
      "  SELECT gid, wegbehsrt, false AS reversed, geom " +
      "  FROM public.nwb " +
      "  WHERE rijrichtng IS NULL OR rijrichtng = 'H' " +
      "  UNION ALL " +
      "  SELECT gid, wegbehsrt, true AS reversed, ST_Reverse(geom) AS geom " +
      "  FROM public.nwb " +
      "  WHERE rijrichtng IS NULL OR rijrichtng = 'T' " +
      ") " +
      "SELECT " +
      "  gid AS id, " +
      "  reversed, " +
      "  ST_Length(ST_Transform(geom, 4326)::geography) AS length_in_meters, " +
      "  ST_AsEWKB(ST_Transform(geom, 4326)) AS geometry_wkb " +
      "FROM nwb_per_direction " +
      "WHERE wegbehsrt = 'R' " +
      "ORDER BY gid, geom")
  @RegisterRowMapper(LineStringLocationMapper.class)
  public List<LineStringLocation> getNwbLocationsOnlyNationalHighways();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.nwb_matches " +
      "( " +
      "  gid integer NOT NULL, " +
      "  reversed boolean NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (gid, reversed), " +
      "  CONSTRAINT nwb_matches_fkey_nwb FOREIGN KEY (gid) " +
      "      REFERENCES public.nwb(gid) " +
      ") "
      )
  void createNwbMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.nwb_matches")
  void truncateNwbMatchesTable();

  @SqlBatch(
      "INSERT INTO public.nwb_matches(gid, reversed, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :reversed, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  int[] insertNwbMatches(@BindBean List<LineStringMatch> lineStringMatches);


}
