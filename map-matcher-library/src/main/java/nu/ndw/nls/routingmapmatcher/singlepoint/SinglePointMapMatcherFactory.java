package nu.ndw.nls.routingmapmatcher.singlepoint;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.bearing.BearingCalculator;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.geometry.mappers.DiameterToPolygonMapper;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SinglePointMapMatcherFactory implements MapMatcherFactory<SinglePointMapMatcher> {

    private final BearingCalculator bearingCalculator;
    private final DiameterToPolygonMapper diameterToPolygonMapper;
    private final GeometryFactoryWgs84 geometryFactoryWgs84;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;

    @Override
    public SinglePointMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {

        return new SinglePointMapMatcher(diameterToPolygonMapper, bearingCalculator,
                geometryFactoryWgs84, fractionAndDistanceCalculator, preInitializedNetwork, profileName);
    }
}
