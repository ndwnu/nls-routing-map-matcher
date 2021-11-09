package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.List;
import java.util.function.Supplier;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;

@Data
@RequiredArgsConstructor
public class MapMatchingRequest<T extends BaseLocation> {

    private final String locationTypeName;
    private final Supplier<List<T>> locationSupplier;
}
