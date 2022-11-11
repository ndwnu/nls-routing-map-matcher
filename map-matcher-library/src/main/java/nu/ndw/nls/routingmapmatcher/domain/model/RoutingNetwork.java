package nu.ndw.nls.routingmapmatcher.domain.model;

import java.util.Iterator;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RoutingNetwork {

    String networkNameAndVersion;
    Supplier<Iterator<Link>> linkSupplier;
}
