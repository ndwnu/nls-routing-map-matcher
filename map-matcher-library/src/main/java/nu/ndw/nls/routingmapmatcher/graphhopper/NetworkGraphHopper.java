package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongLongBTree;
import com.graphhopper.coll.LongLongMap;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathRouter;
import com.graphhopper.storage.index.LocationIndexTree;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static final int BYTES_PER_INTEGER = 4;
    private static final int EMPTY_VALUE = -1;
    private static final String DATAREADER_IMPORT_DATE = "datareader.import.date";
    private static final String DATAREADER_DATA_DATE = "datareader.data.date";

    private Supplier<Iterator<Link>> linkSupplier;
    private Instant dataDate;

    private LongLongMap nodeIdToInternalNodeIdMap;

    public NetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier) {
        this(linkSupplier, null);
    }

    public NetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier, Instant dataDate) {
        this.linkSupplier = linkSupplier;
        this.dataDate = dataDate;
        this.nodeIdToInternalNodeIdMap = new GHLongLongBTree(MAX_LEAF_ENTRIES, BYTES_PER_INTEGER, EMPTY_VALUE);
    }

    /**
     * Loading an existing network from disk does not require a link supplier and nodeIdToInternalNodeIdMap
     */
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
        NetworkReader networkReader = getNetworkReader(this.linkSupplier, nodeIdToInternalNodeIdMap);
        networkReader.readGraph();
        putDateProperty(DATAREADER_IMPORT_DATE, Instant.now());
        if (this.dataDate != null) {
            putDateProperty(DATAREADER_DATA_DATE, this.dataDate);
        }
        this.writeEncodingManagerToProperties();
    }

    protected NetworkReader getNetworkReader(Supplier<Iterator<Link>> linkSupplier,
            LongLongMap nodeIdToInternalNodeIdMap) {
        return new NetworkReader(getBaseGraph().getBaseGraph(), getEncodingManager(), linkSupplier,
                getOSMParsers().getWayTagParsers(), nodeIdToInternalNodeIdMap);
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

    public Instant getImportDate() {
        return getDateProperty(DATAREADER_IMPORT_DATE);
    }

    public Instant getDataDate() {
        return getDateProperty(DATAREADER_DATA_DATE);
    }

    private void putDateProperty(String propertyKey, Instant date) {
        getProperties().put(propertyKey, DateTimeFormatter.ISO_INSTANT.format(date.truncatedTo(ChronoUnit.SECONDS)));
    }

    private Instant getDateProperty(String propertyKey) {
        String date = getProperties().get(propertyKey);
        return date != null ? Instant.parse(date) : null;
    }
}
