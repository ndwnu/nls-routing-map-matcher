package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.util.GeometryHelper;
import org.locationtech.jts.geom.LineString;

public class LinkDeserializer extends StdDeserializer<Link> {

    private final GeometryHelper geometryHelper = new GeometryHelper();

    public LinkDeserializer() {
        this(null);
    }

    protected LinkDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Link deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int id = node.get("id").intValue();
        int fromNodeId = node.get("fromNodeId").intValue();
        int toNodeId = node.get("toNodeId").intValue();
        double speedInKilometersPerHour = node.get("speedInKilometersPerHour").doubleValue();
        double reverseSpeedInKilometersPerHour = node.get("reverseSpeedInKilometersPerHour").doubleValue();
        double distanceInMeters = node.get("distanceInMeters").doubleValue();
        byte[] geometryWkb = node.get("geometry").binaryValue();
        LineString lineString = geometryHelper.convertToLinestring(geometryWkb);

        return Link.builder()
                .id(id)
                .fromNodeId(fromNodeId)
                .toNodeId(toNodeId)
                .speedInKilometersPerHour(speedInKilometersPerHour)
                .reverseSpeedInKilometersPerHour(reverseSpeedInKilometersPerHour)
                .distanceInMeters(distanceInMeters)
                .geometry(lineString)
                .build();
    }
}
