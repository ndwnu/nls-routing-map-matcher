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

public interface WazeIrregularitiesDao {

  @SqlQuery(
      "SELECT irregularity_id AS id, null AS location_index, null AS reversed, length AS length_in_meters, " +
      "ST_AsEWKB(line::geometry) AS geometry_wkb " +
      "FROM public.waze_irregularities " +
      "ORDER BY id")
  @RegisterRowMapper(LineStringLocationMapper.class)
  List<LineStringLocation> getWazeIrregularities();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.waze_irregularities_matches " +
      "( " +
      "  irregularity_id integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (irregularity_id), " +
      "  CONSTRAINT waze_irregularities_matches_fkey_waze_irregularities FOREIGN KEY (irregularity_id) " +
      "      REFERENCES public.waze_irregularities(irregularity_id) " +
      ") "
      )
  void createWazeIrregularitiesMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.waze_irregularities_matches")
  void truncateWazeIrregularitiesMatchesTable();

  @SqlBatch(
      "INSERT INTO public.waze_irregularities_matches(irregularity_id, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  int[] insertWazeIrregularitiesMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
