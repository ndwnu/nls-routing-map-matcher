package nu.ndw.nls.routingmapmatcher.domain.model;


import java.util.function.Supplier;

public class RoutingNetwork {
    private final int networkVersion;
    private final Supplier<Link> linkSupplier;

    public RoutingNetwork(int networkVersion, Supplier<Link> linkSupplier) {
        this.networkVersion = networkVersion;
        this.linkSupplier = linkSupplier;
    }
}
