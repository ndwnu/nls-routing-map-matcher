package nl.dat.routingmapmatcher.dataaccess.dao;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import nl.dat.routingmapmatcher.starttoend.StartToEndMatch;

public interface StartToEndMatchDao {

  @SqlUpdate(
      "CREATE TABLE IF NOT EXISTS public.measurement_site_line_matches " +
      "( " +
      "  measurement_site_id integer NOT NULL, " +
      "  location_index integer NOT NULL, " +
      "  ndw_link_ids integer[] NOT NULL, " +
      "  start_link_fraction double precision, " +
      "  end_link_fraction double precision, " +
      "  reliability double precision, " +
      "  status text, " +
      "  line_string geography(LineString,4326), " +
      "  PRIMARY KEY (measurement_site_id, location_index), " +
      "  CONSTRAINT measurement_site_line_matches_fkey_measurement_sites FOREIGN KEY (measurement_site_id) " +
      "      REFERENCES public.measurement_sites(measurement_site_id) " +
      ") "
      )
  void createMeasurementSiteLocationMatchesTableIfNotExists();

  @SqlUpdate("TRUNCATE TABLE public.measurement_site_line_matches")
  void truncateMeasurementSiteLocationMatchesTable();

  @SqlBatch(
      "INSERT INTO public.measurement_site_line_matches(measurement_site_id, location_index, ndw_link_ids, " +
      "  start_link_fraction, end_link_fraction, reliability, status, line_string) VALUES " +
      "  (:id, :locationIndex, :ndwLinkIds, :startLinkFraction, :endLinkFraction, :reliability, :status, :lineString)")
  int[] insertMeasurementSiteLocationMatches(@BindBean List<StartToEndMatch> startToEndMatches);

}
