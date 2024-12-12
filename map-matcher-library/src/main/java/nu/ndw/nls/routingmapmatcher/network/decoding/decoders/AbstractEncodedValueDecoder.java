package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.EdgeIteratorState;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;

/**
 * Base class that allows you to read values from a network and decode them back into java types
 * @param <T>
 */
abstract class AbstractEncodedValueDecoder<T> implements EncodedValueDecoder<T> {

    /**
     * Retrieves the encoded value from the network and returns it
     *
     * @param networkGraphHopper the graphhopper network
     * @param roadSectionId road section id
     * @param encodedValueName graphhopper encode value name
     * @param reverse true if reverse
     * @return the value as encoded on the network
     */
    public T decode(NetworkGraphHopper networkGraphHopper, long roadSectionId, String encodedValueName, boolean reverse) {
        EncodingManager encodingManager = networkGraphHopper.getEncodingManager();
        EdgeIteratorState edgeIteratorState = getEdgeIteratorState(networkGraphHopper, roadSectionId);

        return retrieveValueFromNetwork(encodingManager, edgeIteratorState, encodedValueName, reverse);
    }

    protected abstract T retrieveValueFromNetwork(EncodingManager encodingManager, EdgeIteratorState edgeIteratorState,
            String encodedValueName, boolean reverse);

    private static EdgeIteratorState getEdgeIteratorState(NetworkGraphHopper networkGraphHopper, Long roadSectionId) {
        Integer edgeKey = networkGraphHopper.getEdgeMap().get(roadSectionId);
        return networkGraphHopper.getBaseGraph().getEdgeIteratorStateForKey(edgeKey);
    }

}
