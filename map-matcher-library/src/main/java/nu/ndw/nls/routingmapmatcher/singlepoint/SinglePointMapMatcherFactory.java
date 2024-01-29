package nu.ndw.nls.routingmapmatcher.singlepoint;

import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
public class SinglePointMapMatcherFactory implements MapMatcherFactory<SinglePointMapMatcher> {

    @Override
    public SinglePointMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new SinglePointMapMatcher(preInitializedNetwork, profileName);
    }
}
