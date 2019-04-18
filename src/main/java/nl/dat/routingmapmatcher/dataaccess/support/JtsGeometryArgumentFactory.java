package nl.dat.routingmapmatcher.dataaccess.support;

import java.sql.Types;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKBWriter;

public class JtsGeometryArgumentFactory extends AbstractArgumentFactory<Geometry> {

  private static final int OUTPUT_DIMENSION = 2;
  private static final boolean INCLUDE_SRID = true;

  private final WKBWriter wkbWriter;

  public JtsGeometryArgumentFactory() {
    super(Types.BINARY);
    wkbWriter = new WKBWriter(OUTPUT_DIMENSION, INCLUDE_SRID);
  }

  @Override
  protected Argument build(final Geometry geometry, final ConfigRegistry config) {
    return (position, statement, ctx) -> statement.setBytes(position, wkbWriter.write(geometry));
  }

}
