package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
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
    public LineStringLocation deserialize(final JsonParser jsonParser,
            final DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final int id = node.get("id").intValue();
        final int locationIndex = node.get("locationIndex").intValue();
        final boolean reversed = node.get("reversed").booleanValue();
        final double lengthInMeters = node.get("lengthInMeters").doubleValue();
        final ReliabilityCalculationType reliabilityCalculationType = ReliabilityCalculationType
                .valueOf(node.get("reliabilityCalculationType").textValue());
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final double upstreamIsochrone = Optional.ofNullable(node.get("upstreamIsochrone"))
                .map(JsonNode::doubleValue).orElse(0.0);
        final IsochroneUnit upstreamIsochroneUnit = Optional.ofNullable(node.get("upstreamIsochroneUnit"))
                .map(JsonNode::textValue).map(IsochroneUnit::valueOf).orElse(null);
        final double downstreamIsochrone = Optional.ofNullable(node.get("downstreamIsochrone"))
                .map(JsonNode::doubleValue).orElse(0.0);
        final IsochroneUnit downstreamIsochroneUnit = Optional.ofNullable(node.get("downstreamIsochroneUnit"))
                .map(JsonNode::textValue).map(IsochroneUnit::valueOf).orElse(null);
        final LineString lineString = geometryHelper.convertToLinestring(geometryWkb);
        return new LineStringLocation(id, locationIndex, reversed, lengthInMeters, lineString,
                reliabilityCalculationType, upstreamIsochrone, upstreamIsochroneUnit, downstreamIsochrone,
                downstreamIsochroneUnit);
    }
}
