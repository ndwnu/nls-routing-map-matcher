package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;
import org.locationtech.jts.geom.Point;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SinglePointLocation extends BaseLocation {

    private static final double DEFAULT_CANDIDATE_DISTANCE_IN_METERS = 20.0;

    private final Point point;
    @Builder.Default
    private final double cutoffDistance = DEFAULT_CANDIDATE_DISTANCE_IN_METERS;
    private final BearingFilter bearingFilter;
}
