package nl.dat.routingmapmatcher.dataaccess.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.dataaccess.dto.LinkDto;

/**
 * A JDBI mapper class for {@link LinkDto} instances.
 */
public class LinkDtoMapper implements RowMapper<LinkDto> {

  private final WKBReader wkbReader;

  public LinkDtoMapper() {
    this.wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID));
  }

  @Override
  public LinkDto map(final ResultSet resultSet, final StatementContext statementContext) throws SQLException {
    final long id = resultSet.getLong("link_id");
    final long fromNodeId = resultSet.getLong("from_node_id");
    final long toNodeId = resultSet.getLong("to_node_id");
    final double speedInKilometersPerHour = resultSet.getDouble("speed_kmh");
    final double reverseSpeedInKilometersPerHour = resultSet.getDouble("reverse_speed_kmh");
    final double distanceInMeters = resultSet.getDouble("distance_in_meters");
    final byte[] geometryWkb = resultSet.getBytes("geometry_wkb");
    final LineString lineString = createLineString(geometryWkb);
    return new LinkDto(id, fromNodeId, toNodeId, speedInKilometersPerHour, reverseSpeedInKilometersPerHour,
        distanceInMeters, lineString);
  }

  private LineString createLineString(final byte[] geometryWkb) throws SQLException {
    try {
      final Geometry geometry = wkbReader.read(geometryWkb);
      if (!(geometry instanceof LineString)) {
        throw new SQLException("Unexpected geometry type: expected LineString");
      }
      return (LineString) geometry;
    } catch (final ParseException e) {
      throw new SQLException("Unable to parse WKB", e);
    }
  }
}
