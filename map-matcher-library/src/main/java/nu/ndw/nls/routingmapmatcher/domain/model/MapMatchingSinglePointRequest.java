package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.List;
import java.util.function.Supplier;
import lombok.Builder;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;


public class MapMatchingSinglePointRequest extends MapMatchingRequest<SinglePointLocation> {

    @Builder
    public MapMatchingSinglePointRequest(final String locationTypeName,
            final Supplier<List<SinglePointLocation>> locationSupplier) {
        super(locationTypeName, locationSupplier);
    }
}
