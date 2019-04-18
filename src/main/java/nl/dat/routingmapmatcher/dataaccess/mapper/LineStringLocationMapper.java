package nl.dat.routingmapmatcher.dataaccess.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.linestring.LineStringLocation;

/**
 * A JDBI mapper class for {@link LineStringLocation} instances.
 */
public class LineStringLocationMapper implements RowMapper<LineStringLocation> {

  private final WKBReader wkbReader;

  public LineStringLocationMapper() {
    this.wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID));
  }

  @Override
  public LineStringLocation map(final ResultSet resultSet, final StatementContext statementContext)
      throws SQLException {
    final int id = resultSet.getInt("id");
    final boolean reversed = resultSet.getBoolean("reversed");
    final double lengthInMeters = resultSet.getDouble("length_in_meters");
    final LineString geometry = createLineString(resultSet.getBytes("geometry_wkb"));
    return new LineStringLocation(id, reversed, lengthInMeters, geometry);
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
