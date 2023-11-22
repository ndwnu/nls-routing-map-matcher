package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.EncodingType.BOOLEAN;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.EncodingType.DECIMAL;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.EncodingType.INT;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.Operator.EQUALS;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.Operator.SMALLER_THAN;

import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.VehicleProperties;

@RequiredArgsConstructor
@Getter
public enum EncodedTag {

    WAY_ID("way_id", INT,
            null, 31, false,
            vp -> null, null),
    MUNICIPALITY_CODE("municipality_code", INT,
            LinkTag.MUNICIPALITY_CODE, 17, false,
            vp -> null, null),
    MAX_LENGTH("max_length", DECIMAL,
            LinkTag.C17_MAX_LENGTH, 7, true,
            VehicleProperties::length, SMALLER_THAN),
    MAX_WIDTH("max_width", DECIMAL,
            LinkTag.C18_MAX_WIDTH, 7, true,
            VehicleProperties::width, SMALLER_THAN),
    MAX_HEIGHT("max_height", DECIMAL,
            LinkTag.C19_MAX_HEIGHT, 7, true,
            VehicleProperties::height, SMALLER_THAN),
    MAX_AXLE_LOAD("max_axle_load", DECIMAL,
            LinkTag.C20_MAX_AXLE_LOAD, 7, true,
            VehicleProperties::axleLoad, SMALLER_THAN),
    MAX_WEIGHT("max_weight", DECIMAL,
            LinkTag.C21_MAX_WEIGHT, 7, true,
            VehicleProperties::weight, SMALLER_THAN),
    // Here, car means any vehicle with more than 2 wheels. It includes bus, HGV, LCV and tractor, but not motorcycle.
    CAR_ACCESS_FORBIDDEN("car_access_forbidden", BOOLEAN,
            LinkTag.C6_CAR_ACCESS_FORBIDDEN, null, true,
            vp -> vp.carAccessForbidden() ? true : null, EQUALS),
    HGV_ACCESS_FORBIDDEN("hgv_access_forbidden", BOOLEAN,
            LinkTag.C7_HGV_ACCESS_FORBIDDEN, null, true,
            vp -> vp.hgvAccessForbidden() ? true : null, EQUALS),
    BUS_ACCESS_FORBIDDEN("bus_access_forbidden", BOOLEAN,
            LinkTag.C7A_BUS_ACCESS_FORBIDDEN, null, true,
            vp -> vp.busAccessForbidden() ? true : null, EQUALS),
    HGV_AND_BUS_ACCESS_FORBIDDEN("hgv_and_bus_access_forbidden", BOOLEAN,
            LinkTag.C7B_HGV_AND_BUS_ACCESS_FORBIDDEN, null, true,
            vp -> vp.hgvAndBusAccessForbidden() ? true : null, EQUALS),
    TRACTOR_ACCESS_FORBIDDEN("tractor_access_forbidden", BOOLEAN,
            LinkTag.C8_TRACTOR_ACCESS_FORBIDDEN, null, true,
            vp -> vp.tractorAccessForbidden() ? true : null, EQUALS),
    SLOW_VEHICLE_ACCESS_FORBIDDEN("slow_vehicle_access_forbidden", BOOLEAN,
            LinkTag.C9_SLOW_VEHICLE_ACCESS_FORBIDDEN, null, true,
            vp -> vp.slowVehicleAccessForbidden() ? true : null, EQUALS),
    TRAILER_ACCESS_FORBIDDEN("trailer_access_forbidden", BOOLEAN,
            LinkTag.C10_TRAILER_ACCESS_FORBIDDEN, null, true,
            vp -> vp.trailerAccessForbidden() ? true : null, EQUALS),
    MOTORCYCLE_ACCESS_FORBIDDEN("motorcycle_access_forbidden", BOOLEAN,
            LinkTag.C11_MOTORCYCLE_ACCESS_FORBIDDEN, null, true,
            vp -> vp.motorcycleAccessForbidden() ? true : null, EQUALS),
    MOTOR_VEHICLE_ACCESS_FORBIDDEN("motor_vehicle_access_forbidden", BOOLEAN,
            LinkTag.C12_MOTOR_VEHICLE_ACCESS_FORBIDDEN, null, true,
            vp -> vp.motorVehicleAccessForbidden() ? true : null, EQUALS),
    LCV_AND_HGV_ACCESS_FORBIDDEN("lcv_and_hgv_access_forbidden", BOOLEAN,
            LinkTag.C22C_LCV_AND_HGV_ACCESS_FORBIDDEN, null, true,
            vp -> vp.lcvAndHgvAccessForbidden() ? true : null, EQUALS);

    private static final String NONEXISTENT_TAG_MSG =
            "No tag exists with label \"%s\". New tags can be added in the routing-map-matcher library when needed.";

    private final String key;
    private final EncodingType encodingType;
    private final LinkTag<?> linkTag;
    private final Integer bits;
    private final boolean separateValuesPerDirection;
    private final Function<VehicleProperties, ?> valueFunction;
    private final Operator operator;

    public static EncodedTag withKey(String key) {
        return Stream.of(EncodedTag.values())
                .filter(value -> value.getKey().equals(key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(NONEXISTENT_TAG_MSG.formatted(key)));
    }

    public enum EncodingType {
        STRING,
        INT,
        DECIMAL,
        BOOLEAN
    }

    public enum Operator {
        SMALLER_THAN,
        EQUALS
    }
}
