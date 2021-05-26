package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.reader.DataReader;
import com.graphhopper.storage.GraphHopperStorage;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;

import java.util.Iterator;
import java.util.function.Supplier;

public class NetworkGraphHopper extends GraphHopper {

    private final Supplier<Iterator<Link>> linkSupplier;

    private final LongIntMap nodeIdToInternalNodeIdMap;

    public NetworkGraphHopper(final Supplier<Iterator<Link>> linkSupplier) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = new GHLongIntBTree(200);
    }

    @Override
    protected DataReader createReader(final GraphHopperStorage ghStorage) {
        return initDataReader(new NetworkReader(ghStorage, linkSupplier, nodeIdToInternalNodeIdMap));
    }

}
