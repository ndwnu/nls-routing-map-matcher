package nu.ndw.nls.routingmapmatcher.config;

import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.StartToEndMapMatcher;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint.GraphHopperSinglePointMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.starttoend.GraphHopperStartToEndMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.ViterbiLinestringMapMatcherFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(RoutingMapMatcher.class)
public class MapMatcherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RoutingMapMatcher routingMapMatcher(
            final MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory,
            final MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherMapMatcherFactory) {
        return new RoutingMapMatcher(lineStringMapMatcherFactory, singlePointMapMatcherMapMatcherFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkGraphHopperFactory networkGraphHopperFactory() {
        return new NetworkGraphHopperFactory();
    }

    @Bean
    @ConditionalOnMissingBean(value = LineStringMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<LineStringMapMatcher> lineStringMapMatcherFactory() {
        return new ViterbiLinestringMapMatcherFactory(
                new NetworkGraphHopperFactory()
        );
    }

    @Bean
    @ConditionalOnMissingBean(value = SinglePointMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<SinglePointMapMatcher> singlePointMapMatcherFactory() {
        return new GraphHopperSinglePointMapMatcherFactory(
                new NetworkGraphHopperFactory()
        );
    }

    @Bean
    @ConditionalOnMissingBean(value = StartToEndMapMatcher.class, parameterizedContainer = MapMatcherFactory.class)
    public MapMatcherFactory<StartToEndMapMatcher> startToEndMapMatcherFactory() {
        return new GraphHopperStartToEndMapMatcherFactory(
                new NetworkGraphHopperFactory()
        );
    }
}
