package nl.dat.routingmapmatcher.dataaccess.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

import nl.dat.routingmapmatcher.constants.GlobalConstants;
import nl.dat.routingmapmatcher.dataaccess.dto.SituationRecordLocationDto;


/**
 * A JDBI mapper class for {@link SituationRecordLineDto} instances.
 */
public class SituationRecordLineDtoMapper implements RowMapper<SituationRecordLocationDto> {

  private final WKBReader wkbReader;

  public SituationRecordLineDtoMapper() {
    this.wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID));
  }

  @Override
  public SituationRecordLocationDto map(final ResultSet resultSet, final StatementContext statementContext)
      throws SQLException {
    final int id = resultSet.getInt("situation_record_id");
    final int locationIndex = resultSet.getInt("location_index");
    if (resultSet.getString("location_type").equals("point")) {
      final Point locationForDisplay = createPoint(resultSet.getBytes("location_wkb"));
      return new SituationRecordLocationDto(id, locationIndex, locationForDisplay);
    } else if (resultSet.getString("location_type").equals("linear")) {
      final Point startPoint = createPoint(resultSet.getBytes("start_point_wkb"));
      final Point endPoint = createPoint(resultSet.getBytes("end_point_wkb"));
      return new SituationRecordLocationDto(id, locationIndex, startPoint, endPoint);
    }
    return null;
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
