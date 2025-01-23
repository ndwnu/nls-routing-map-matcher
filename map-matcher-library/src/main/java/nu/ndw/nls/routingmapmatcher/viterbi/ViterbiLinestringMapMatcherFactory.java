package nu.ndw.nls.routingmapmatcher.viterbi;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.geometry.distance.FractionAndDistanceCalculator;
import nu.ndw.nls.geometry.factories.GeometryFactoryWgs84;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.PointListUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class ViterbiLinestringMapMatcherFactory implements MapMatcherFactory<ViterbiLineStringMapMatcher> {

    private final GeometryFactoryWgs84 geometryFactoryWgs84;
    private final FractionAndDistanceCalculator fractionAndDistanceCalculator;
    private final PointListUtil pointListUtil;
    @Value("${nls.routing.map-matcher.score.weighting.factor:1.0}")
    private double absoluteRelativeWeighingFactor;

    @Override
    public ViterbiLineStringMapMatcher createMapMatcher(NetworkGraphHopper preInitializedNetwork, String profileName) {
        return new ViterbiLineStringMapMatcher(preInitializedNetwork, profileName, geometryFactoryWgs84,
                fractionAndDistanceCalculator, pointListUtil, absoluteRelativeWeighingFactor);
    }
}
