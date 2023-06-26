package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongIntBTree;
import com.graphhopper.coll.LongIntMap;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathRouter;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.Helper;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.Network;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


@Slf4j
public class NetworkGraphHopper extends GraphHopper implements Network {

    private static final int MAX_LEAF_ENTRIES = 200;
    private static final String DATAREADER_IMPORT_DATE = "datareader.import.date";

    private Supplier<Iterator<Link>> linkSupplier;

    private LongIntMap nodeIdToInternalNodeIdMap;

    public NetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier) {
        this.linkSupplier = linkSupplier;
        this.nodeIdToInternalNodeIdMap = new GHLongIntBTree(MAX_LEAF_ENTRIES);

    }

    /**
     * Loading an existing network from disk does not require a link supplier and nodeIdToInternalNodeIdMap
     **/
    public NetworkGraphHopper() {

    }

    /**
     * GraphHopper uses this method importOSM for loading a network from file on r 862, this is a hardcoded method name
     * we need to override in order to load a network from a linkSupplier. Via a linkSupplier we can load any map type
     * into graphhopper.
     */
    @Override
    protected void importOSM() {
        log.info("Start creating graph from db ");
        this.createBaseGraphAndProperties();
        NetworkReader networkReader = new NetworkReader(getBaseGraph().getBaseGraph(), getEncodingManager(),
                this.linkSupplier, getOSMParsers().getWayTagParsers(),
                nodeIdToInternalNodeIdMap);
        networkReader.readGraph();
        DateFormat f = Helper.createFormatter();
        getProperties().put(DATAREADER_IMPORT_DATE, f.format(new Date()));
        this.writeEncodingManagerToProperties();
    }

    @Override
    public LocationIndexTree getLocationIndex() {
        return (LocationIndexTree) super.getLocationIndex();
    }

    /**
     * calcPaths method in order to get access to the edges and id's via the paths This is used to get the link ids and
     * determine the start and end fractions. In the previous version (0.12) of graphhopper this was part of the GH
     * routing api.
     *
     * @param request the gh routing request
     * @return a list of path objects
     * @see <a
     * href="https://github.com/graphhopper/graphhopper/blob/0.12/core/src/main/java/com/graphhopper/GraphHopper.java#L929">original
     * routing api</a>
     */
    public List<Path> calcPaths(GHRequest request) {
        Map<String, Profile> profilesByName = getProfiles()
                .stream().collect(Collectors
                        .toMap(Profile::getName,
                                Function.identity(),
                                (e1, e2) -> e1,
                                LinkedHashMap::new
                        ));

        return new PathRouter(getBaseGraph(),
                getEncodingManager(),
                getLocationIndex(), profilesByName,
                getPathDetailsBuilderFactory(),
                getTranslationMap(),
                getRouterConfig(),
                createWeightingFactory(),
                getCHGraphs(), getLandmarks())
                .calcPaths(request);
    }
}
