package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Builder;
import lombok.Value;

@Value
public class BearingRange {

    double minBearing;
    double maxBearing;
}
