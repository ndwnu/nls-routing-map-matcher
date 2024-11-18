package nu.ndw.nls.routingmapmatcher.model.routing;

import com.graphhopper.util.CustomModel;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.locationtech.jts.geom.Point;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingRequest {
    private final String routingProfile;
    private final List<Point> wayPoints;
    private final CustomModel customModel;
    @Builder.Default
    private final boolean simplifyResponseGeometry = true;
    @Builder.Default
    private final boolean snapToNodes = false;
}
