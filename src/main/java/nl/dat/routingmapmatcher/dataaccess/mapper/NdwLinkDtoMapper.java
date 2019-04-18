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
import nl.dat.routingmapmatcher.dataaccess.dto.NdwLinkDto;

/**
 * A JDBI mapper class for {@link NdwLinkDto} instances.
 */
public class NdwLinkDtoMapper implements RowMapper<NdwLinkDto> {

  private final WKBReader wkbReader;

  public NdwLinkDtoMapper() {
    this.wkbReader = new WKBReader(new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID));
  }

  @Override
  public NdwLinkDto map(final ResultSet resultSet, final StatementContext statementContext) throws SQLException {
    final int forwardId = resultSet.getInt("forward_id");
    final int backwardId = resultSet.getInt("backward_id");
    final boolean forwardAccess = resultSet.getBoolean("forward_access");
    final boolean backwardAccess = resultSet.getBoolean("backward_access");
    final int fromNodeId = resultSet.getInt("from_node_id");
    final int toNodeId = resultSet.getInt("to_node_id");
    final byte[] geometryWkb = resultSet.getBytes("geometry_wkb");
    final LineString lineString = createLineString(geometryWkb);
    return new NdwLinkDto(forwardId, backwardId, forwardAccess, backwardAccess, fromNodeId, toNodeId, lineString);
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
