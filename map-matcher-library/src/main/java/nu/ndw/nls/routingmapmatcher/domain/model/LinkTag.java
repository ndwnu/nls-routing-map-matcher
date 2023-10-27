package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class LinkTag<T> {

    public static final LinkTag<Double> MAX_HEIGHT = new LinkTag<>("max-height", Double.class, true);
    public static final LinkTag<Double> MAX_WIDTH = new LinkTag<>("max-width", Double.class, true);
    public static final LinkTag<Double> MAX_WEIGHT = new LinkTag<>("max-weight", Double.class, true);
    public static final LinkTag<Double> MAX_LENGTH = new LinkTag<>("max-length", Double.class, true);
    public static final LinkTag<Double> MAX_AXLE_LOAD = new LinkTag<>("max-axle-load", Double.class, true);
    public static final LinkTag<Integer> MUNICIPALITY_CODE = new LinkTag<>("municipality-code", Integer.class, false);
    public static final LinkTag<Boolean> HGV_ACCESSIBLE = new LinkTag<>("hgv-accessible", Boolean.class, true);
    public static final LinkTag<Boolean> BUS_ACCESSIBLE = new LinkTag<>("bus-accessible", Boolean.class, true);
    public static final LinkTag<Boolean> GEN_ACCESSIBLE = new LinkTag<>("gen-accessible", Boolean.class, false);

    public static final LinkTag<Boolean> HGV_ACCESS_FORBIDDEN = new LinkTag<>("hgv-access-forbidden", Boolean.class,
            true);
    public static final LinkTag<Boolean> CAR_ACCESS_FORBIDDEN = new LinkTag<>("car-access-forbidden", Boolean.class,
            true);
    public static final LinkTag<Boolean> AUTO_BUS_ACCESS_FORBIDDEN = new LinkTag<>("auto-bus-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Boolean> TRAILER_ACCESS_FORBIDDEN = new LinkTag<>("trailer-access-forbidden",
            Boolean.class, true);


    public static final String FORWARD_SUFFIX = ":forward";
    public static final String REVERSE_SUFFIX = ":reverse";

    private final String label;
    private final Class<T> clazz;
    private final boolean separateValuesPerDirection;

}
