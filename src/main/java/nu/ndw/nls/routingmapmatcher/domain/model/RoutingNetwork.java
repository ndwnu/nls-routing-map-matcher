package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Iterator;
import java.util.function.Supplier;

@Value
@Builder
public class RoutingNetwork {
    String networkNameAndVersion;
    Supplier<Iterator<Link>> linkSupplier;
}
