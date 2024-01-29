package nu.ndw.nls.routingmapmatcher.network;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.graphhopper.GHRequest;
import com.graphhopper.GraphHopper;
import com.graphhopper.coll.GHLongLongBTree;
import com.graphhopper.coll.LongLongMap;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.PathRouter;
import com.graphhopper.routing.util.parsers.TagParser;
import com.graphhopper.storage.index.LocationIndexTree;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.exception.GraphHopperFlushException;
import nu.ndw.nls.routingmapmatcher.network.init.NetworkReader;
import nu.ndw.nls.routingmapmatcher.network.model.Link;
import nu.ndw.nls.routingmapmatcher.network.model.RoutingNetworkSettings;

@Slf4j
public class NetworkGraphHopper extends GraphHopper {

    private static final int MAX_LEAF_ENTRIES = 200;
    private static final int BYTES_PER_INTEGER = 4;
    private static final int EMPTY_VALUE = -1;
    private static final String DATAREADER_IMPORT_DATE = "datareader.import.date";
    private static final String DATAREADER_DATA_DATE = "datareader.data.date";

    private final Supplier<Iterator<? extends Link>> linkSupplier;
    private final Instant dataDate;

    private final LongLongMap nodeIdToInternalNodeIdMap;
    private final boolean indexed;
    private boolean storeOnFlush;

    @Getter
    private final boolean expandBounds;

    @Getter
    private HashMap<Long, Integer> edgeMap = new HashMap<>();

    public NetworkGraphHopper(RoutingNetworkSettings routingNetworkSettings) {
        this.linkSupplier = routingNetworkSettings.getLinkSupplier();
        this.dataDate = routingNetworkSettings.getDataDate();
        this.nodeIdToInternalNodeIdMap = new GHLongLongBTree(MAX_LEAF_ENTRIES, BYTES_PER_INTEGER, EMPTY_VALUE);
        this.expandBounds = routingNetworkSettings.isExpandBounds();
        this.indexed = routingNetworkSettings.isIndexed();
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
        List<TagParser> wayTagParsers = getOSMParsers().getWayTagParsers();
        NetworkReader networkReader = new NetworkReader(getBaseGraph(), this.linkSupplier, wayTagParsers,
                nodeIdToInternalNodeIdMap, this.edgeMap, expandBounds);
        networkReader.readGraph();
        putDateProperty(DATAREADER_IMPORT_DATE, Instant.now());
        if (this.dataDate != null) {
            putDateProperty(DATAREADER_DATA_DATE, this.dataDate);
        }
        this.writeEncodingManagerToProperties();
    }

    @Override
    public NetworkGraphHopper setStoreOnFlush(boolean storeOnFlush) {
        super.setStoreOnFlush(storeOnFlush);
        this.storeOnFlush = storeOnFlush;
        return this;
    }

    @Override
    public void flush() {
        super.flush();

        if (indexed && storeOnFlush) {
            try (FileOutputStream outputStream = new FileOutputStream(getEdgeMapFilePath())) {
                Output output = new Output(outputStream);
                getKryo().writeObject(output, this.edgeMap);
                output.close();
            } catch (IOException e) {
                throw new GraphHopperFlushException("Error saving EdgeMap to file", e);
            }
        }
    }

    @Override
    public boolean load() {
        return indexed ? (super.load() && loadEdgeMap()) : super.load();
    }

    private boolean loadEdgeMap() {
        try (FileInputStream inputStream = new FileInputStream(getEdgeMapFilePath())) {
            Input input = new Input(inputStream);
            this.edgeMap = getKryo().readObject(input, HashMap.class);
            input.close();
            return true;
        } catch (IOException e) {
            log.warn("Error loading EdgeMap for network", e);
            return false;
        }
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

    private File getEdgeMapFilePath() {
        return new File(this.getGraphHopperLocation(), "edgemap");
    }

    private Kryo getKryo() {
        Kryo kryo = new Kryo();
        kryo.register(HashMap.class);
        return kryo;
    }
}
