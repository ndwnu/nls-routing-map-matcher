package nu.ndw.nls.routingmapmatcher.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MapMatchingSinglePointRequest extends MapMatchingRequest<SinglePointLocation> {

}
