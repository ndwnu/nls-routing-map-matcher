package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.storage.index.QueryResult;
import java.lang.reflect.Constructor;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.Isochrone.IsoLabel;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IsochroneMatchMapperTest {

    private static final int MAX_DISTANCE = 200;
    @Mock
    private CrsTransformer crsTransformer;
    @Mock
    private QueryResult startSegment;
    @Mock
    private QueryGraph queryGraph;
    @Mock
    private LinkFlagEncoder flagEncoder;
    @Mock
    private FractionAndDistanceCalculator fractionAndDistanceCalculator;

    private IsochroneMatchMapper isochroneMatchMapper;

    private IsoLabel isoLabel;


    @BeforeEach
    void setup() {
        isochroneMatchMapper = IsochroneMatchMapper
                .builder()
                .crsTransformer(crsTransformer)
                .flagEncoder(flagEncoder)
                .maxDistance(MAX_DISTANCE)
                .fractionAndDistanceCalculator(fractionAndDistanceCalculator)
                .queryGraph(queryGraph)
                .startSegment(startSegment)
                .build();
    }

    @Test
    void when_isolabel_is_start_segment_mapToIsochroneMatch_should_return_ok() {
        int edgeId = 1;
        int adjNode = 2;
        double weight = 10;
        long time = 10;
        double distance = 10;
        isoLabel = createIsoLabel(edgeId, adjNode, weight, time, distance);

    }

    @SneakyThrows
    private static IsoLabel createIsoLabel(int edgeId,
            int adjNode,
            double weight,
            long time,
            double distance) {
        Constructor<IsoLabel> constructor = IsoLabel.class.getDeclaredConstructor(
                int.class,
                int.class,
                double.class,
                long.class,
                double.class);
        constructor.setAccessible(true);
        return constructor.newInstance(edgeId, adjNode, weight, time, distance);
    }
}
