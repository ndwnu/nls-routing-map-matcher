package nu.ndw.nls.routingmapmatcher.model.linestring;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.LineString;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public class MatchedEdgeLink {

    private final int linkId;
    private final boolean reversed;
    private final double distance;
    private final LineString geometry;
    private final LineString originalGeometry;
}
