package nu.ndw.nls.routingmapmatcher.routing;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.mappers.MatchedLinkMapper;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouterFactory implements MapMatcherFactory<Router> {

    private final MatchedLinkMapper matchedLinkMapper;

    @Override
    public Router createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new Router(preInitializedNetwork, matchedLinkMapper);
    }
}
