package nu.ndw.nls.routingmapmatcher.domain.model.starttoend;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
@Getter
@ToString
public class StartToEndLocation {

    private final int id;
    private final int locationIndex;
    private final double lengthAffected;
    private final Point startPoint;
    private final Point endPoint;
}
