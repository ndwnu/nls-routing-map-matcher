package nu.ndw.nls.routingmapmatcher.graphhopper;


import com.graphhopper.coll.LongLongMap;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.BaseGraph;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.Coordinate;

@Slf4j
public class ExpandedBoundsNetworkReader extends NetworkReader {

    private static final double BOUND_EXPAND = .000001;

    public ExpandedBoundsNetworkReader(BaseGraph baseGraph, EncodingManager encodingManager,
            Supplier<Iterator<Link>> linkSupplier, List<TagParser> vehicleTagParsers,
            LongLongMap nodeIdToInternalNodeIdMap) {
        super(baseGraph, encodingManager, linkSupplier, vehicleTagParsers, nodeIdToInternalNodeIdMap);
    }

    @Override
    protected OptionalInt addLink(Link link) {
        OptionalInt edgeKey = super.addLink(link);
        if(edgeKey.isPresent()) {
            Coordinate[] coordinates = link.getGeometry().getCoordinates();
            Arrays.stream(coordinates).forEach(coord -> {
                baseGraph.getBounds().update(coord.y + BOUND_EXPAND, coord.x + BOUND_EXPAND);
                baseGraph.getBounds().update(coord.y - BOUND_EXPAND, coord.x - BOUND_EXPAND);
            });
        }
        return edgeKey;
    }
}
