package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import org.locationtech.jts.geom.LineString;

import java.io.IOException;

import static nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.GeometryHelper.convertToLinestring;

public class LinkDeserializer extends StdDeserializer<Link> {


    public LinkDeserializer() {
        this(null);

    }

    protected LinkDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Link deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        final JsonNode node = jsonParser.getCodec()
                .readTree(jsonParser);
        final int id = node.get("id").intValue();
        final int fromNodeId = node.get("fromNodeId").intValue();
        final int toNodeId = node.get("toNodeId").intValue();
        final double speedInKilometersPerHour = node.get("speedInKilometersPerHour").doubleValue();
        final double reverseSpeedInKilometersPerHour = node.get("reverseSpeedInKilometersPerHour").doubleValue();
        final double distanceInMeters = node.get("distanceInMeters").doubleValue();
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final LineString lineString = convertToLinestring(geometryWkb);

        return new Link(id,
                fromNodeId,
                toNodeId,
                speedInKilometersPerHour,
                reverseSpeedInKilometersPerHour,
                distanceInMeters,
                lineString);
    }


}
