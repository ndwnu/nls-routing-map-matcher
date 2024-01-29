package nu.ndw.nls.routingmapmatcher.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;

@SuperBuilder
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MapMatchingLineRequest extends MapMatchingRequest<LineStringLocation> {

    @Builder.Default
    LineMatchingMode lineMatchingMode = LineMatchingMode.LINE_STRING;
}
