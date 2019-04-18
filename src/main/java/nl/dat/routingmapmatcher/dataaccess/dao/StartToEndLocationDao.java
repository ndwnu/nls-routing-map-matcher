package nl.dat.routingmapmatcher.dataaccess.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import nl.dat.routingmapmatcher.dataaccess.mapper.StartToEndLocationMapper;
import nl.dat.routingmapmatcher.starttoend.StartToEndLocation;

public interface StartToEndLocationDao {

  @SqlQuery(
      "SELECT " +
      "  measurement_site_locations.measurement_site_id AS id, " +
      "  measurement_site_locations.location_index, " +
      "  measurement_site_locations.length_affected, " +
      "  ST_AsEWKB(measurement_site_locations.linear_coordinates_start_point::geometry) AS start_point, " +
      "  ST_AsEWKB(measurement_site_locations.linear_coordinates_end_point::geometry) AS end_point " +
      "FROM measurement_site_locations " +
      "JOIN measurement_sites USING (measurement_site_id) " +
      "WHERE " +
      "  measurement_sites.equipment NOT IN ('fcd', 'fcd ') AND " +
      "  measurement_site_locations.linear_coordinates_start_point IS NOT NULL AND " +
      "  measurement_site_locations.linear_coordinates_end_point IS NOT NULL AND " +
      "  measurement_sites.id NOT IN (SELECT dgl_loc FROM measurement_site_lines_shapefile) " +
      "ORDER BY id, location_index"
      )
  @RegisterRowMapper(StartToEndLocationMapper.class)
  public List<StartToEndLocation> getNoFcdStartToEndMeasurementLocations();

}
