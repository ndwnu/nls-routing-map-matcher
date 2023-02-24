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

    protected LinkDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public Link deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final int id = node.get("id").intValue();
        final int fromNodeId = node.get("fromNodeId").intValue();
        final int toNodeId = node.get("toNodeId").intValue();
        final double speedInKilometersPerHour = node.get("speedInKilometersPerHour").doubleValue();
        final double reverseSpeedInKilometersPerHour = node.get("reverseSpeedInKilometersPerHour").doubleValue();
        final double distanceInMeters = node.get("distanceInMeters").doubleValue();
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final LineString lineString = geometryHelper.convertToLinestring(geometryWkb);

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
