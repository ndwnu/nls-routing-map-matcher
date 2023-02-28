package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.ReliabilityCalculationType;
import nu.ndw.nls.routingmapmatcher.util.GeometryHelper;
import org.locationtech.jts.geom.LineString;

public class LineStringLocationDeserializer extends StdDeserializer<LineStringLocation> {

    private final GeometryHelper geometryHelper = new GeometryHelper();

    public LineStringLocationDeserializer() {
        this(null);
    }

    protected LineStringLocationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LineStringLocation deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        int id = node.get("id").intValue();
        int locationIndex = node.get("locationIndex").intValue();
        boolean reversed = node.get("reversed").booleanValue();
        double lengthInMeters = node.get("lengthInMeters").doubleValue();
        ReliabilityCalculationType reliabilityCalculationType = ReliabilityCalculationType
                .valueOf(node.get("reliabilityCalculationType").textValue());
        byte[] geometryWkb = node.get("geometry").binaryValue();
        double upstreamIsochrone = Optional.ofNullable(node.get("upstreamIsochrone"))
                .map(JsonNode::doubleValue).orElse(0.0);
        IsochroneUnit upstreamIsochroneUnit = Optional.ofNullable(node.get("upstreamIsochroneUnit"))
                .map(JsonNode::textValue).map(IsochroneUnit::valueOf).orElse(null);
        double downstreamIsochrone = Optional.ofNullable(node.get("downstreamIsochrone"))
                .map(JsonNode::doubleValue).orElse(0.0);
        IsochroneUnit downstreamIsochroneUnit = Optional.ofNullable(node.get("downstreamIsochroneUnit"))
                .map(JsonNode::textValue).map(IsochroneUnit::valueOf).orElse(null);
        LineString lineString = geometryHelper.convertToLinestring(geometryWkb);
        return LineStringLocation.builder()
                .id(id)
                .locationIndex(locationIndex)
                .reversed(reversed)
                .lengthInMeters(lengthInMeters)
                .geometry(lineString)
                .reliabilityCalculationType(reliabilityCalculationType)
                .upstreamIsochrone(upstreamIsochrone)
                .upstreamIsochroneUnit(upstreamIsochroneUnit)
                .downstreamIsochrone(downstreamIsochrone)
                .downstreamIsochroneUnit(downstreamIsochroneUnit)
                .build();
    }
}
