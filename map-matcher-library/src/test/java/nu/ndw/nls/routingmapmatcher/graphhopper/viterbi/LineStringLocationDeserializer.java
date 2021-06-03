package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import nu.ndw.nls.routingmapmatcher.util.GeometryHelper;
import org.locationtech.jts.geom.LineString;

import java.io.IOException;
import java.util.Optional;

public class LineStringLocationDeserializer extends StdDeserializer<LineStringLocation> {

    private final GeometryHelper geometryHelper = new GeometryHelper();

    public LineStringLocationDeserializer() {
        this(null);
    }

    protected LineStringLocationDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public LineStringLocation deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {

        final JsonNode node = jsonParser.getCodec()
                .readTree(jsonParser);
        final int id = node.get("id").intValue();
        final boolean reversed = node.get("reversed").booleanValue();
        final double lengthInMeters = node.get("lengthInMeters").doubleValue();
        final ReliabilityCalculationType reliabilityCalculationType = ReliabilityCalculationType
                .valueOf(node.get("reliabilityCalculationType").textValue());
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final LineString lineString = geometryHelper.convertToLinestring(geometryWkb);
        return new LineStringLocation(id,
                Optional.empty(),
                Optional.of(reversed),
                lengthInMeters, lineString,
                reliabilityCalculationType);
    }


}
