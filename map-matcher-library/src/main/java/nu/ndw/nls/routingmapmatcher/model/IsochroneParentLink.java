package nu.ndw.nls.routingmapmatcher.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class IsochroneParentLink {

    private final int linkId;
    private final boolean reversed;
}
