package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.EncodingType.DECIMAL;
import static nu.ndw.nls.routingmapmatcher.graphhopper.ev.EncodedTag.EncodingType.INT;

import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;

@RequiredArgsConstructor
@Getter
public enum EncodedTag {

    WAY_ID("way_id", INT, null, 31, false, vd -> null),
    MUNICIPALITY_CODE("municipality_code", INT, LinkTag.MUNICIPALITY_CODE, 17, false, vd -> null),
    MAX_AXLE_LOAD("max_axle_load", DECIMAL, LinkTag.MAX_AXLE_LOAD, 7, true, VehicleDimensions::axleLoad),
    MAX_HEIGHT("max_height", DECIMAL, LinkTag.MAX_HEIGHT, 7, true, VehicleDimensions::height),
    MAX_LENGTH("max_length", DECIMAL, LinkTag.MAX_LENGTH, 7, true, VehicleDimensions::length),
    MAX_WEIGHT("max_weight", DECIMAL, LinkTag.MAX_WEIGHT, 7, true, VehicleDimensions::weight),
    MAX_WIDTH("max_width", DECIMAL, LinkTag.MAX_WIDTH, 7, true, VehicleDimensions::width);

    private static final String NONEXISTENT_TAG_MSG =
            "No tag exists with label \"%s\". New tags can be added in the routing-map-matcher library when needed.";

    private final String key;
    private final EncodingType encodingType;
    private final LinkTag<?> linkTag;
    private final Integer bits;
    private final boolean separateValuesPerDirection;
    private final Function<VehicleDimensions, Double> valueFunction;

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

}
