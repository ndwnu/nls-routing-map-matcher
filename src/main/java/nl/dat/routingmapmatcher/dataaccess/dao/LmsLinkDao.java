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

public interface LmsLinkDao {

  /*
   * Read LMS links that are no connectors (linktype = 99), are not in another country (prv_code = 0)
   * and are not part of a future alternative.
   */
  @SqlQuery(
      "SELECT gid AS id, null AS location_index, null AS reversed, ST_Length(ST_Transform(geom,28992)) AS length_in_meters, "
      + " ST_AsEWKB(geom) AS geometry_wkb "
      + " FROM public.lms_links "
      + " WHERE os_linktyp <> 99 "
      + " AND os_jaar < 2019 "
      + " AND os_prv_cod > 0 "
      + " ORDER BY id ")
  @RegisterRowMapper(LineStringLocationMapper.class)
  public List<LineStringLocation> getLmsLinks();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.lms_link_matches " +
      "( " +
      "  gid integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (gid), " +
      "  CONSTRAINT lms_link_matches_fkey_lms_links FOREIGN KEY (gid) " +
      "      REFERENCES public.lms_links(gid) " +
      ") "
      )
  void createLmsLinkMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.lms_link_matches")
  void truncateLmsLinkMatchesTable();

  @SqlBatch(
      "INSERT INTO public.lms_link_matches(gid, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  void insertLmsLinkMatches(@BindBean List<LineStringMatch> lineStringMatches);

}
