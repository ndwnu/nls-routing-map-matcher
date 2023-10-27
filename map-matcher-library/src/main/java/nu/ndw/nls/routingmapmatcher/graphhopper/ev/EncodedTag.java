package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

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

    WAY_ID("way_id", INT, null, 31, false, vd -> null, null),
    MUNICIPALITY_CODE("municipality_code", INT, LinkTag.MUNICIPALITY_CODE, 17, false, vd -> null, null),
    MAX_AXLE_LOAD("max_axle_load", DECIMAL, LinkTag.MAX_AXLE_LOAD, 7, true, VehicleProperties::axleLoad, SMALLER_THAN),
    MAX_HEIGHT("max_height", DECIMAL, LinkTag.MAX_HEIGHT, 7, true, VehicleProperties::height, SMALLER_THAN),
    MAX_LENGTH("max_length", DECIMAL, LinkTag.MAX_LENGTH, 7, true, VehicleProperties::length, SMALLER_THAN),
    MAX_WEIGHT("max_weight", DECIMAL, LinkTag.MAX_WEIGHT, 7, true, VehicleProperties::weight, SMALLER_THAN),
    MAX_WIDTH("max_width", DECIMAL, LinkTag.MAX_WIDTH, 7, true, VehicleProperties::width, SMALLER_THAN),

    HGV_ACCESS_FORBIDDEN("hgv_access_forbidden", EncodingType.BOOLEAN, LinkTag.HGV_ACCESS_FORBIDDEN, null, true,
            VehicleProperties::hgvAccessForbidden, EQUALS),

    CAR_ACCESS_FORBIDDEN("car_access_forbidden", EncodingType.BOOLEAN, LinkTag.CAR_ACCESS_FORBIDDEN, null, true,
            VehicleProperties::carAccessForbidden, EQUALS),

    AUTO_BUS_ACCESS_FORBIDDEN("auto_bus_access_forbidden", EncodingType.BOOLEAN, LinkTag.AUTO_BUS_ACCESS_FORBIDDEN,
            null, true,
            VehicleProperties::autoBusAccessForbidden, EQUALS),

    TRAILER_ACCESS_FORBIDDEN("trailer_access_forbidden", EncodingType.BOOLEAN, LinkTag.TRAILER_ACCESS_FORBIDDEN, null,
            true,
            VehicleProperties::autoBusAccessForbidden, EQUALS);

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
