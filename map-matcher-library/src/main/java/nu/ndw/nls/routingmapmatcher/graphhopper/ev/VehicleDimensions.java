package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import lombok.Builder;

@Builder
public record VehicleDimensions(Double length, Double width, Double height, Double weight, Double axleLoad) {

//    TODO: rename to VehicleProperties, add VehicleType field and replace dimension fields with EnumMap using new enum
//    VehicleProperty. The field 'valueFunction' in EncodedTag can be replaced with a similar field in VehicleProperty,
//    and VehicleDimensionModel can be adapted accordingly.

}
