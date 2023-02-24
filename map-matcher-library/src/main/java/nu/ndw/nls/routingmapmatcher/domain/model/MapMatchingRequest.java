package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.List;
import java.util.function.Supplier;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import nu.ndw.nls.routingmapmatcher.domain.model.base.BaseLocation;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public class MapMatchingRequest<T extends BaseLocation> {

    private final String locationTypeName;
    private final Supplier<List<T>> locationSupplier;
}
