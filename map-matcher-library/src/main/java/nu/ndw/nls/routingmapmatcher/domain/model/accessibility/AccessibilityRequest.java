package nu.ndw.nls.routingmapmatcher.domain.model.accessibility;

import lombok.Builder;
import org.locationtech.jts.geom.Point;

@Builder
public record AccessibilityRequest(Point startPoint, int municipalityId, double travellingDistanceInMetres,
                                   VehicleProperties vehicleProperties) {


}
