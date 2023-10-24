package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.graphhopper.coll.LongLongMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nu.ndw.nls.routingmapmatcher.domain.exception.GraphHopperFlushException;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;


@Slf4j
public class IndexedNetworkGraphHopper extends NetworkGraphHopper {

    private boolean storeOnFlush;

    @Getter
    private HashMap<Long, Integer> edgeMap = new HashMap<>();

    public IndexedNetworkGraphHopper(Supplier<Iterator<Link>> linkSupplier) {
        super(linkSupplier);
    }

    /**
     * Loading an existing network from disk does not require a link supplier and nodeIdToInternalNodeIdMap
     **/
    public IndexedNetworkGraphHopper() {

    }

    @Override
    protected NetworkReader getNetworkReader(Supplier<Iterator<Link>> linkSupplier,
            LongLongMap nodeIdToInternalNodeIdMap) {
        return new IndexedNetworkReader(getBaseGraph().getBaseGraph(), getEncodingManager(),
                linkSupplier, getOSMParsers().getWayTagParsers(),
                nodeIdToInternalNodeIdMap, this.edgeMap);
    }

    @Override
    public IndexedNetworkGraphHopper setStoreOnFlush(boolean storeOnFlush) {
        super.setStoreOnFlush(storeOnFlush);
        this.storeOnFlush = storeOnFlush;
        return this;
    }

    @Override
    public void flush() {
        super.flush();

        if(storeOnFlush) {
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
        return super.load() && loadEdgeMap();
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

    private File getEdgeMapFilePath() {
        return new File(this.getGraphHopperLocation(), "edgemap");
    }

    private Kryo getKryo() {
        Kryo kryo = new Kryo();
        kryo.register(HashMap.class);
        return kryo;
    }

}
