package nu.ndw.nls.routingmapmatcher.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import nu.ndw.nls.routingmapmatcher.util.GeometryHelper;
import org.locationtech.jts.geom.LineString;

public class LinkDeserializer extends StdDeserializer<TestLink> {

    private final GeometryHelper geometryHelper = new GeometryHelper();

    public LinkDeserializer() {
        this(null);
    }

    protected LinkDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public TestLink deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        long id = node.get("id").longValue();
        JsonNode node2 = node.get("linkIdReversed");
        Long linkIdReversed = node2 == null || node2.isNull() ? null : node2.longValue();
        long fromNodeId = node.get("fromNodeId").longValue();
        long toNodeId = node.get("toNodeId").longValue();
        double speedInKilometersPerHour = node.get("speedInKilometersPerHour").doubleValue();
        double reverseSpeedInKilometersPerHour = node.get("reverseSpeedInKilometersPerHour").doubleValue();
        double distanceInMeters = node.get("distanceInMeters").doubleValue();
        byte[] geometryWkb = node.get("geometry").binaryValue();
        LineString lineString = geometryHelper.convertToLinestring(geometryWkb);

        return TestLink.builder()
                .id(id)
                .linkIdReversed(linkIdReversed)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speedInKilometersPerHour)
                .reverseSpeedInKilometersPerHour(reverseSpeedInKilometersPerHour)
                .distanceInMeters(distanceInMeters)
                .geometry(lineString)
                .build();
    }
}
