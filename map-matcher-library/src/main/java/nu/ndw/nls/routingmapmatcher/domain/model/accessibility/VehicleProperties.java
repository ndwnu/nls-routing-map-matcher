package nu.ndw.nls.routingmapmatcher.domain.model.accessibility;

import lombok.Builder;

@Builder
public record VehicleProperties(Double length, Double width, Double height, Double axleLoad, Double weight,
                                Boolean carAccessForbidden, Boolean hgvAccessForbidden, Boolean busAccessForbidden,
                                Boolean hgvAndBusAccessForbidden, Boolean tractorAccessForbidden,
                                Boolean slowVehicleAccessForbidden, Boolean trailerAccessForbidden,
                                Boolean motorcycleAccessForbidden, Boolean motorVehicleAccessForbidden,
                                Boolean lcvAndHgvAccessForbidden) {

}
