package nu.ndw.nls.routingmapmatcher.routing;

import com.graphhopper.util.CustomModel;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.mappers.MatchedLinkMapper;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RouterFactory implements MapMatcherFactory<Router> {

    private final MatchedLinkMapper matchedLinkMapper;
    private final GeometryFactoryWgs84 geometryFactoryWgs84;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    @Override
    public Router createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new Router(preInitializedNetwork, matchedLinkMapper, geometryFactoryWgs84,
                fractionAndDistanceCalculator, profileName, null);
    }

    @Override
    public Router createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName, CustomModel customModel) {
        return new Router(preInitializedNetwork, matchedLinkMapper, geometryFactoryWgs84,
                fractionAndDistanceCalculator, profileName, customModel);
    }
}
