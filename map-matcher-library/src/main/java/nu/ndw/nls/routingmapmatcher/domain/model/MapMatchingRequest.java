package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Builder;
import lombok.Value;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;

import java.util.List;
import java.util.function.Supplier;

@Value
@Builder
public class MapMatchingRequest {
    String name;
    Supplier<List<LineStringLocation>> locationSupplier;
}
