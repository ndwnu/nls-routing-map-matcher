package nu.ndw.nls.routingmapmatcher.config;

import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.RoutingMapMatcher;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopperFactory;
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
    public RoutingMapMatcher routingMapMatcher() {
        return new RoutingMapMatcher(lineStringMapMatcherFactory());
    }

    private LineStringMapMatcherFactory lineStringMapMatcherFactory() {
        return new ViterbiLinestringMapMatcherFactory(
                new NetworkGraphHopperFactory()
        );
    }
}
