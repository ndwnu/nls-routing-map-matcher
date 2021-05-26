package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.google.common.base.Supplier;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.LineStringMapMatcherFactory;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class ViterbiLinestringMapMatcherFactory  implements LineStringMapMatcherFactory {


    private static final Logger logger = LoggerFactory.getLogger(ViterbiLinestringMapMatcherFactory.class);


    @Override
    public LineStringMapMatcher createLineStringMapMatcher(Supplier<Iterator<Link>> linkSupplier) {
        return new ViterbiLineStringMapMatcher(readNetwork(linkSupplier));
    }

    private NetworkGraphHopper readNetwork(Supplier<Iterator<Link>> linkSupplier) {
        //logger.info("Start reading network with version {}", networkVersion);
        final NetworkRepository networkRepository = new NetworkRepository();
        return networkRepository.getNetwork(linkSupplier);
    }
}
