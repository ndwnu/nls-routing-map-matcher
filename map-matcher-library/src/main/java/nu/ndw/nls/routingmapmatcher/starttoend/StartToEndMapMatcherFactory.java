package nu.ndw.nls.routingmapmatcher.starttoend;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartToEndMapMatcherFactory implements MapMatcherFactory<StartToEndMapMatcher> {

    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    @Override
    public StartToEndMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new StartToEndMapMatcher(preInitializedNetwork, profileName, fractionAndDistanceCalculator);
    }
}
