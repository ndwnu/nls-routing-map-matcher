package nl.dat.routingmapmatcher.enums;

public enum NdwNetworkSubset {

  OSM_FULL_NETWORK(getOsmQuery("")),
  OSM_ONLY_NATIONAL_HIGHWAYS(getOsmQuery("WHERE clazz IN (11, 12) ")),
  OSM_NO_SMALL_LINKS(getOsmQuery("WHERE clazz NOT IN (41, 42, 43) ")),
  NWB(getNwbQuery());

  private final String networkQuery;

  NdwNetworkSubset(String networkQuery) {
    this.networkQuery = networkQuery;
  }

  public String getNetworkQuery() {
    return networkQuery;
  }

  private static String getOsmQuery(String whereClause) {
    return
        "SELECT " +
        "  id AS link_id, " +
        "  source AS from_node_id, " +
        "  target AS to_node_id, " +
        "  kmh AS speed_kmh, " +
        "  0 AS reverse_speed_kmh, " +
        "  km * 1000 AS distance_in_meters, " +
        "  ST_AsEWKB(ST_Transform(geom, 4326)) AS geometry_wkb " +
        "FROM basemaps.segments_210401 " +
        whereClause +
        "ORDER BY link_id ASC";
  }

  private static String getNwbQuery() {
    return
        "SELECT " +
        "  wvk_id::integer AS link_id, " +
        "  jte_id_beg::integer AS from_node_id, " +
        "  jte_id_end::integer AS to_node_id, " +
        "  CASE " +
        "    WHEN rijrichtng = 'T' THEN 0 " +
        "    WHEN wegbehsrt = 'R' THEN 100 " +
        "    WHEN wegbehsrt = 'P' THEN 80 " +
        "    ELSE 50 " +
        "  END AS speed_kmh, " +
        "  CASE " +
        "    WHEN rijrichtng = 'H' THEN 0 " +
        "    WHEN wegbehsrt = 'R' THEN 100 " +
        "    WHEN wegbehsrt = 'P' THEN 80 " +
        "    ELSE 50 " +
        "  END AS reverse_speed_kmh, " +
        "  ST_Length(geom) AS distance_in_meters, " +
        "  ST_AsEWKB(ST_Transform(geom, 4326)) AS geometry_wkb " +
        "FROM nwb_wegvakken " +
        "ORDER BY link_id ASC";
  }
}
