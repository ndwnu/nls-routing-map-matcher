package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.util.EdgeIteratorState;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.springframework.stereotype.Component;

@Component
public class EncodedValueDistanceDecoder {

    public double decode(NetworkGraphHopper networkGraphHopper, long roadSectionId) {
        Integer i = networkGraphHopper.getWayIdToEdgeKey().get(roadSectionId);
        EdgeIteratorState edgeIteratorState = networkGraphHopper.getBaseGraph().getEdgeIteratorStateForKey(i);
        return edgeIteratorState.getDistance();
    }

}
