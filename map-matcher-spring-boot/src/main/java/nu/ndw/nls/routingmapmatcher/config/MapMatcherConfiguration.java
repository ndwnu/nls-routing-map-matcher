package nu.ndw.nls.routingmapmatcher.config;

import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.Router;
import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.routing.GraphHopperRouterFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint.GraphHopperSinglePointMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.starttoend.GraphHopperStartToEndMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.ViterbiLinestringMapMatcherFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RoutingMapMatcher.class)
public class MapMatcherConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RoutingMapMatcher routingMapMatcher(
            MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory,
            MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherMapMatcherFactory,
            MapMatcherFactory<StartToEndMapMatcher> startToEndMapMatcherMapMatcherFactory) {
        return new RoutingMapMatcher(lineStringMapMatcherFactory, singlePointMapMatcherMapMatcherFactory,
                startToEndMapMatcherMapMatcherFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkGraphHopperFactory networkGraphHopperFactory() {
        return new NetworkGraphHopperFactory();
    }

    @Bean
    @ConditionalOnMissingBean(value = LineStringMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory(
            NetworkGraphHopperFactory networkGraphHopperFactory) {
        return new ViterbiLinestringMapMatcherFactory(networkGraphHopperFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = SinglePointMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherFactory(
            NetworkGraphHopperFactory networkGraphHopperFactory) {
        return new GraphHopperSinglePointMapMatcherFactory(networkGraphHopperFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = StartToEndMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<StartToEndMapMatcher> startToEndMapMatcherFactory(
            NetworkGraphHopperFactory networkGraphHopperFactory) {
        return new GraphHopperStartToEndMapMatcherFactory(networkGraphHopperFactory);
    }

    @Bean
    @ConditionalOnMissingBean(value = Router.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<Router> routerFactory(
            NetworkGraphHopperFactory networkGraphHopperFactory) {
        return new GraphHopperRouterFactory(networkGraphHopperFactory);
    }
}