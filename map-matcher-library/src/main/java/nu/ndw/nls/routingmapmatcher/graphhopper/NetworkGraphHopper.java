package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.util.Helper;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


@Slf4j
public class NetworkGraphHopper extends GraphHopper implements Network {

    private static final int MAX_LEAF_ENTRIES = 200;

    private final Supplier<Iterator<Link>> linkSupplier;

    private final LongIntMap nodeIdToInternalNodeIdMap;

    public NetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = new GHLongIntBTree(MAX_LEAF_ENTRIES);

    }

    @Override
    protected void importOSM() {
        log.info("Start creating graph from db ");
        this.createBaseGraphAndProperties();
        NetworkReader networkReader = new NetworkReader(getBaseGraph().getBaseGraph(), getEncodingManager(),
                this.linkSupplier, getOSMParsers().getWayTagParsers(),
                nodeIdToInternalNodeIdMap);
        networkReader.readGraph();
        DateFormat f = Helper.createFormatter();
        getProperties().put("datareader.import.date", f.format(new Date()));
        this.writeEncodingManagerToProperties();
    }


}
