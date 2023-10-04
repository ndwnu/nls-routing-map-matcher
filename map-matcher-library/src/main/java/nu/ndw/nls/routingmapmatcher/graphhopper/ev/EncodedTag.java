package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.LinkTag;

@RequiredArgsConstructor
@Getter
public enum EncodedTag {

    WAY_ID("way_id", null),
    MAX_WEIGHT("max_weight", LinkTag.MAX_WEIGHT),
    MAX_WIDTH("max_width", LinkTag.MAX_WIDTH),
    MAX_LENGTH("max_length", LinkTag.MAX_LENGTH),
    MAX_AXLE_LOAD("max_axle_load", LinkTag.MAX_AXLE_LOAD),
    MAX_HEIGHT("max_height", LinkTag.MAX_HEIGHT),
    MUNICIPALITY_CODE("municipality_code", LinkTag.MUNICIPALITY_CODE),
    HGV_ACCESSIBLE("hgv_accessible", LinkTag.HGV_ACCESSIBLE);

    private final String key;
    private final LinkTag linkTag;

    public static EncodedTag withKey(String key) {
        return Stream.of(EncodedTag.values())
                .filter(value -> value.getKey().equals(key))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

}
