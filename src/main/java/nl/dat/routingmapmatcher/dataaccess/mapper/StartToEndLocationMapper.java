package nl.dat.routingmapmatcher.dataaccess.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.starttoend.StartToEndLocation;

/**
 * A JDBI mapper class for {@link StartToEndLocation} instances.
 */
public class StartToEndLocationMapper implements RowMapper<StartToEndLocation> {

  private final WKBReader wkbReader;

  public StartToEndLocationMapper() {
    this.wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID));
  }

  @Override
  public StartToEndLocation map(final ResultSet resultSet, final StatementContext statementContext)
      throws SQLException {
    final int id = resultSet.getInt("id");
    final int locationIndex = resultSet.getInt("location_index");
    final double lengthAffected = resultSet.getDouble("length_affected");
    final Point startPoint = createPoint(resultSet.getBytes("start_point"));
    final Point endPoint = createPoint(resultSet.getBytes("end_point"));
    return new StartToEndLocation(id, locationIndex, lengthAffected, startPoint, endPoint);
  }

  private Point createPoint(final byte[] geometryWkb) throws SQLException {
    try {
      final Geometry geometry = wkbReader.read(geometryWkb);
      if (!(geometry instanceof Point)) {
        throw new SQLException("Unexpected geometry type: expected Point");
      }
      return (Point) geometry;
    } catch (final ParseException e) {
      throw new SQLException("Unable to parse WKB", e);
    }
  }
}
