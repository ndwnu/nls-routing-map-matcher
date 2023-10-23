package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.coll.LongLongMap;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;

@Slf4j
public class IndexedNetworkReader extends NetworkReader {

    private final Map<Long, Integer> edgeMap;

    public IndexedNetworkReader(BaseGraph baseGraph, EncodingManager encodingManager,
            Supplier<Iterator<Link>> linkSupplier, List<TagParser> vehicleTagParsers,
            LongLongMap nodeIdToInternalNodeIdMap, Map<Long, Integer> edgeMap) {
        super(baseGraph, encodingManager, linkSupplier, vehicleTagParsers, nodeIdToInternalNodeIdMap);
        this.edgeMap = edgeMap;
    }

    @Override
    protected int addLink(Link link) {
        int edgeKey = super.addLink(link);
        edgeMap.put(link.getId(), edgeKey);
        return edgeKey;
    }
}
