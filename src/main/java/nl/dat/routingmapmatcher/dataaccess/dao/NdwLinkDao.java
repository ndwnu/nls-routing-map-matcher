package nl.dat.routingmapmatcher.dataaccess.dao;

import java.util.Iterator;

import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.FetchSize;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import nl.dat.routingmapmatcher.dataaccess.dto.NdwLinkDto;
import nl.dat.routingmapmatcher.dataaccess.mapper.NdwLinkDtoMapper;
import nl.dat.routingmapmatcher.enums.NdwNetworkSubset;

public interface NdwLinkDao {

  /**
   * Return NDW base network links in such a way that a link can have two possible driving directions.
   * <p>
   * As the NDW base network always has separate links for each driving direction, an extra step is needed to
   * combine links that have the same geometry when one of the geometries would be reversed.
   * <p>
   * The query first makes sure that geometries always point in the same direction. This is done by ensuring the
   * source node always has an id with a lesser value than the id of the target node. If this is not the case, the
   * link is reversed. In case of a loop (the source node id equals the target node id) the geometry must be in
   * clockwise direction.
   * <p>
   * After making sure that geometries are always in the same direction, the group by clause will combine links
   * with the same geometry.
   *
   * @param subset subset of NDW base network to read
   * @return iterator with NDW base network links
   */
  @FetchSize(10_000) // Only takes effect in PostgreSQL when called in a transaction
  @SqlQuery(
      "WITH normalized_network AS ( " +
      "  SELECT " +
      "    id, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        id ELSE -1 END AS forward_id, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        -1 ELSE id END AS backward_id, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        true ELSE false END AS forward_access, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        false ELSE true END AS backward_access, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        source ELSE target END AS from_node_id, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        target ELSE source END AS to_node_id, " +
      "    CASE WHEN (source < target) OR (source = target AND ST_IsPolygonCW(ST_MakePolygon(geom))) THEN " +
      "        geom ELSE ST_Reverse(geom) END AS geometry " +
      "  FROM basemaps.segments_190101 " +
      "  WHERE " +
      "    :subset = 'FULL_NETWORK' OR " +
      "    (:subset = 'ONLY_NATIONAL_HIGHWAYS' AND clazz IN (11,12)) OR " +
      "    (:subset = 'NO_SMALL_LINKS' AND clazz NOT IN (41,42,43)) " +
      "  ORDER BY id " +
      ") " +
      "SELECT " +
      "  MAX(forward_id) AS forward_id, " +
      "  MAX(backward_id) AS backward_id, " +
      "  bool_or(forward_access) AS forward_access, " +
      "  bool_or(backward_access) AS backward_access, " +
      "  from_node_id, " +
      "  to_node_id, " +
      "  ST_AsEWKB(ST_Transform(geometry, 4326)) AS geometry_wkb " +
      "FROM normalized_network " +
      "GROUP BY from_node_id, to_node_id, geometry " +
      "ORDER BY GREATEST(MAX(forward_id), MAX(backward_id)) "
      )
  @RegisterRowMapper(NdwLinkDtoMapper.class)
  public Iterator<NdwLinkDto> getNdwLinksIterator(@Bind("subset") NdwNetworkSubset subset);

}
