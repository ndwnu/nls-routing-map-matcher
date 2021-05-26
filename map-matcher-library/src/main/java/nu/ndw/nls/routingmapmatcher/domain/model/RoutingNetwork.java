package nu.ndw.nls.routingmapmatcher.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.Iterator;
import java.util.function.Supplier;

@Value
@Builder
public class RoutingNetwork {
    int networkVersion;
    Supplier<Iterator<Link>> linkSupplier;
}
