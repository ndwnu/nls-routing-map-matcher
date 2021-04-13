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

public interface WazeJamsDao {

  @SqlQuery(
      "SELECT jam_id AS id, null AS location_index, null AS reversed, length AS length_in_meters, " +
      "ST_AsEWKB(line::geometry) AS geometry_wkb " +
      "FROM public.waze_jams " +
      "ORDER BY id")
  @RegisterRowMapper(LineStringLocationMapper.class)
  List<LineStringLocation> getWazeJams();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.waze_jams_matches " +
      "( " +
      "  jam_id integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (jam_id), " +
      "  CONSTRAINT waze_jams_matches_fkey_waze_jams FOREIGN KEY (jam_id) " +
      "      REFERENCES public.waze_jams(jam_id) " +
      ") "
      )
  void createWazeJamsMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.waze_jams_matches")
  void truncateWazeJamsMatchesTable();

  @SqlBatch(
      "INSERT INTO public.waze_jams_matches(jam_id, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  int[] insertWazeJamsMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
