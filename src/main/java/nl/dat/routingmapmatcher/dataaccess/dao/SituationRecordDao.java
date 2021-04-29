package nl.dat.routingmapmatcher.dataaccess.dao;

import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import nl.dat.routingmapmatcher.dataaccess.dto.SituationRecordLocationDto;
import nl.dat.routingmapmatcher.dataaccess.mapper.SituationRecordLineDtoMapper;
import nl.dat.routingmapmatcher.linestring.LineStringMatch;

public interface SituationRecordDao {

  // Excluded are situation record locations (location_type = 'point') without location of display.
  // There is only 1 in the current xml-file (26-3-2019)
  // Run SQL query in postgres to check the number of excluded locations.
  @SqlQuery("SELECT "
      + " situation_record_id, "
      + " location_index, "
      + " location_type, "
      + " ST_AsEWKB(situation_record_locations.location_for_display::geometry) AS location_wkb, "
      + " ST_AsEWKB(linear_coordinates_start_point::geometry) AS start_point_wkb, "
      + " ST_AsEWKB(linear_coordinates_end_point::geometry) AS end_point_wkb "
      + " FROM public.situation_record_locations "
      + " INNER JOIN public.situation_records USING (situation_record_id) "
      + " WHERE ordered_locations = true "
      + " AND situation_records.location_for_display IS NOT NULL "
      + " ORDER BY situation_record_id, location_index ")
  @RegisterRowMapper(SituationRecordLineDtoMapper.class)
  Iterator<SituationRecordLocationDto> getSituationRecordOrderedLocations();

  // Excluded are situation record locations (location_type = 'linear') without locations for start or end point.
  // There are none in the current xml-file (26-3-2019)
  // Run SQL query in postgres to check the number of excluded locations.
  @SqlQuery("SELECT "
      + " situation_record_id, "
      + " location_index, "
      + " location_type, "
      + " ST_AsEWKB(linear_coordinates_start_point::geometry) AS start_point_wkb, "
      + " ST_AsEWKB(linear_coordinates_end_point::geometry) AS end_point_wkb "
      + " FROM public.situation_record_locations "
      + " INNER JOIN public.situation_records USING (situation_record_id) "
      + " WHERE ordered_locations = false "
      + " AND location_type = 'linear' "
      + " AND linear_coordinates_start_point IS NOT NULL "
      + " AND linear_coordinates_end_point IS NOT NULL "
      + " ORDER BY situation_record_id, location_index ")
  @RegisterRowMapper(SituationRecordLineDtoMapper.class)
  Iterator<SituationRecordLocationDto> getSituationRecordUnorderedLinears();

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.situation_record_line_matches " +
      "( " +
      "  situation_record_id integer NOT NULL, " +
      "  location_index integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (situation_record_id, location_index), " +
      "  CONSTRAINT situation_record_line_matches_fkey_situation_records FOREIGN KEY (situation_record_id) " +
      "      REFERENCES public.situation_records(situation_record_id) " +
      ") "
      )
  void createSituationRecordLineMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.situation_record_line_matches")
  void truncateSituationRecordLineMatchesTable();

  @SqlBatch(
      "INSERT INTO public.situation_record_line_matches(situation_record_id, location_index, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :locationIndex, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  void insertSituationRecordLineMatches(@BindBean List<LineStringMatch> lineStringMatches);
}
