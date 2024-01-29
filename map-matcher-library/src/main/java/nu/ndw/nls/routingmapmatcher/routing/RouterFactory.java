package nu.ndw.nls.routingmapmatcher.routing;

import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
public class RouterFactory implements MapMatcherFactory<Router> {

    @Override
    public Router createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new Router(preInitializedNetwork);
    }
}
