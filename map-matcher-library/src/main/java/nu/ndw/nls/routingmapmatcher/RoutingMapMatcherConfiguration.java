package nu.ndw.nls.routingmapmatcher;

import nu.ndw.nls.geometry.GeometryConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ComponentScan
@Import(GeometryConfiguration.class)
public class RoutingMapMatcherConfiguration {

}
