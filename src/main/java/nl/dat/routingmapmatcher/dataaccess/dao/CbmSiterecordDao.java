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

public interface CbmSiterecordDao {

  @SqlQuery("SELECT " +
      "  id, " +
      "  NULL AS location_index, " +
      "  false AS reversed, " +
      "  ST_Length(line_shape::geography) AS length_in_meters, " +
      "  ST_AsEWKB(line_shape) AS geometry_wkb " +
      "FROM public.cbm_siterecord " +
      "WHERE equipment_type_used = 'fcd' AND location_type = 'linear' AND line_shape IS NOT NULL " +
      "ORDER BY id")
  @RegisterRowMapper(LineStringLocationMapper.class)
  List<LineStringLocation> getCbmSiterecords();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.cbm_siterecord_matches " +
          "( " +
          "  id integer NOT NULL PRIMARY KEY, " +
          "  reversed boolean NOT NULL, " +
          "  fcd_link_ids integer[] NOT NULL, " +
          "  start_link_fraction double precision, " +
          "  end_link_fraction double precision, " +
          "  reliability double precision, " +
          "  status text, " +
          "  line_string geography(LineString,4326), " +
          "  CONSTRAINT cbm_siterecord_matches_fkey_cbm_siterecord FOREIGN KEY (id) " +
          "      REFERENCES public.cbm_siterecord(id) " +
          ") "
  )
  void createCbmSiterecordMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.cbm_siterecord_matches")
  void truncateCbmSiterecordMatchesTable();

  @SqlBatch(
      "INSERT INTO public.cbm_siterecord_matches(id, reversed, fcd_link_ids, " +
          "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
          "  (:id, :reversed, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  void insertCbmSiterecordMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
