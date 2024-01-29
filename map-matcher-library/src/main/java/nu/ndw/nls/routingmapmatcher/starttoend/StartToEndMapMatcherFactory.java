package nu.ndw.nls.routingmapmatcher.starttoend;

import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
public class StartToEndMapMatcherFactory implements MapMatcherFactory<StartToEndMapMatcher> {

    @Override
    public StartToEndMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new StartToEndMapMatcher(preInitializedNetwork, profileName);
    }
}
