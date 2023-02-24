package nu.ndw.nls.routingmapmatcher.domain.model.singlepoint;

import lombok.Value;

@Value
public class BearingFilter {

    int target;
    int cutoffMargin;
}
