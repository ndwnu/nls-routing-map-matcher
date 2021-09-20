package nu.ndw.nls.routingmapmatcher.domain.model.starttoend;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Point;

@RequiredArgsConstructor
@Getter
public class StartToEndLocation {

    private final int id;
    private final int locationIndex;
    private final double lengthAffected;
    private final Point startPoint;
    private final Point endPoint;
}
