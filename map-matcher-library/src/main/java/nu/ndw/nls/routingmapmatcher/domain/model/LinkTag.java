package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public final class LinkTag<T> {

    public static final LinkTag<Double> C19_MAX_HEIGHT = new LinkTag<>("max-height", Double.class, true);
    public static final LinkTag<Double> C18_MAX_WIDTH = new LinkTag<>("max-width", Double.class, true);
    public static final LinkTag<Double> C21_MAX_WEIGHT = new LinkTag<>("max-weight", Double.class, true);
    public static final LinkTag<Double> C17_MAX_LENGTH = new LinkTag<>("max-length", Double.class, true);
    public static final LinkTag<Double> C20_MAX_AXLE_LOAD = new LinkTag<>("max-axle-load", Double.class, true);
    public static final LinkTag<Boolean> C7_HGV_ACCESS_FORBIDDEN = new LinkTag<>("hgv-access-forbidden", Boolean.class,
            true);
    public static final LinkTag<Boolean> C6_CAR_ACCESS_FORBIDDEN = new LinkTag<>("car-access-forbidden", Boolean.class,
            true);
    public static final LinkTag<Boolean> C7A_AUTO_BUS_ACCESS_FORBIDDEN = new LinkTag<>("auto-bus-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Boolean> C10_TRAILER_ACCESS_FORBIDDEN = new LinkTag<>("trailer-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Boolean> C7B_HGV_AND_AUTO_BUS_ACCESS_FORBIDDEN = new LinkTag<>("hgv-and-auto-bus-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Boolean> C11_MOTOR_BIKE_ACCESS_FORBIDDEN = new LinkTag<>("motor-bike-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Boolean> C12_MOTOR_VEHICLE_ACCESS_FORBIDDEN = new LinkTag<>("motor-vehicle-access-forbidden",
            Boolean.class, true);
    public static final LinkTag<Integer> MUNICIPALITY_CODE = new LinkTag<>("municipality-code", Integer.class, false);
    public static final String FORWARD_SUFFIX = ":forward";
    public static final String REVERSE_SUFFIX = ":reverse";

    private final String label;
    private final Class<T> clazz;
    private final boolean separateValuesPerDirection;

}
