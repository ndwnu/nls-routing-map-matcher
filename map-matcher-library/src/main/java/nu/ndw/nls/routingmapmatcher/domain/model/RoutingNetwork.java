package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.Iterator;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@Getter
@EqualsAndHashCode
@ToString
public class RoutingNetwork {

    private final String networkNameAndVersion;
    private final Supplier<Iterator<Link>> linkSupplier;
}
