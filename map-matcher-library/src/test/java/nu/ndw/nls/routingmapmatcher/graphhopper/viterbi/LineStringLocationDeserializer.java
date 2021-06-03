package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import org.locationtech.jts.geom.LineString;

import java.io.IOException;
import java.util.Optional;

import static nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.GeometryHelper.convertToLinestring;

public class LineStringLocationDeserializer extends StdDeserializer<LineStringLocation> {


    public LineStringLocationDeserializer() {
        this(null);
    }

    protected LineStringLocationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LineStringLocation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {

        final JsonNode node = jsonParser.getCodec()
                .readTree(jsonParser);
        final int id = node.get("id").intValue();
        final boolean reversed = node.get("reversed").booleanValue();
        final double lengthInMeters = node.get("lengthInMeters").doubleValue();
        final ReliabilityCalculationType reliabilityCalculationType = ReliabilityCalculationType
                .valueOf(node.get("reliabilityCalculationType").textValue());
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final LineString lineString = convertToLinestring(geometryWkb);
        return new LineStringLocation(id,
                Optional.empty(),
                Optional.of(reversed),
                lengthInMeters, lineString,
                reliabilityCalculationType);
    }


}
