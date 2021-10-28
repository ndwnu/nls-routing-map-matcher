package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;


public class MapMatchingLineRequest extends MapMatchingRequest<LineStringLocation> {

    @Builder
    public MapMatchingLineRequest(String locationTypeName,
            Supplier<List<LineStringLocation>> locationSupplier) {
        super(locationTypeName, locationSupplier);
    }
}
