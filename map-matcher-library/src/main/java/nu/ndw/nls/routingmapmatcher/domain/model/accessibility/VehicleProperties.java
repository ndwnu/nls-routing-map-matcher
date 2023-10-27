package nu.ndw.nls.routingmapmatcher.domain.model.accessibility;

import lombok.Builder;

@Builder
public record VehicleProperties(Double length,
                                Double width,
                                Double height,
                                Double weight,
                                Double axleLoad,
                                Boolean hgvAccessForbidden,
                                Boolean carAccessForbidden,
                                Boolean autoBusAccessForbidden,
                                Boolean trailerAccessForbidden
                                ) {

}
