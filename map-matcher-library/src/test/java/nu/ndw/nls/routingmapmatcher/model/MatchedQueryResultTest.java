package nu.ndw.nls.routingmapmatcher.model;

import static org.assertj.core.api.Assertions.assertThat;

import nu.ndw.nls.routingmapmatcher.util.GeometryConstants;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

class MatchedQueryResultTest {

    private static final double LONG_1 = 5.358247;
    private static final double LAT_1 = 52.161257;
    private static final double LONG_2 = 5.379687;
    private static final double LAT_2 = 52.158304;
    private static final double LONG_3 = 5.379667;
    private static final double LAT_3 = 52.158280;
    private static final double LONG_4 = 5.3783483;
    private static final double LAT_4 = 52.1590774;
    private static final Coordinate coordinateA1 = new Coordinate(LONG_1, LAT_1);
    private static final Coordinate coordinateA2 = new Coordinate(LONG_2, LAT_2);
    private static final Coordinate coordinateA3 = new Coordinate(LONG_3, LAT_3);
    private static final Coordinate coordinateA4 = new Coordinate(LONG_4, LAT_4);

    @Test
    void getCutoffGeometriesAsLineStrings_ok() {
        var geometryFactory = GeometryConstants.WGS84_GEOMETRY_FACTORY;
        var lineString_1 = geometryFactory
                .createLineString(new Coordinate[]{coordinateA1, coordinateA2, coordinateA3});
        var lineString_2 = geometryFactory
                .createLineString(new Coordinate[]{coordinateA3, coordinateA4});
        var multilineString = geometryFactory
                .createMultiLineString(new LineString[]{lineString_1, lineString_2});
        var matchedQueryresult = MatchedQueryResult
                .builder()
                .cutoffGeometry(multilineString)
                .build();
        var result = matchedQueryresult.getCutoffGeometryAsLineStrings();
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(lineString_1);
        assertThat(result.get(1)).isEqualTo(lineString_2);
    }
}
