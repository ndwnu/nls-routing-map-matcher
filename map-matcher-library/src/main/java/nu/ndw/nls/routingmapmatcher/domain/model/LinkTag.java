package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum LinkTag {
    MAX_HEIGHT("max-height", Double.class),
    MAX_WIDTH("max-width", Double.class),
    MAX_WEIGHT("max-weight", Double.class),
    MAX_LENGTH("max-length", Double.class),
    MAX_AXLE_LOAD("max-axle-load", Double.class),
    MUNICIPALITY_CODE("municipality-code", Integer.class),
    HGV_ACCESSIBLE("hgv-accessible", Boolean.class);
    private final String label;
    private final Class<?> clazz;
}
