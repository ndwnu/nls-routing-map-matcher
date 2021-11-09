package nu.ndw.nls.routingmapmatcher.graphhopper.viterbi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.util.GeometryHelper;
import org.locationtech.jts.geom.Point;

public class SinglePointLocationDeserializer extends StdDeserializer<SinglePointLocation> {

    private final GeometryHelper geometryHelper = new GeometryHelper();

    public SinglePointLocationDeserializer() {
        this(null);
    }

    protected SinglePointLocationDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public SinglePointLocation deserialize(final JsonParser jsonParser,
            final DeserializationContext deserializationContext) throws IOException {
        final JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        final int id = node.get("id").intValue();
        final byte[] geometryWkb = node.get("geometry").binaryValue();
        final Point point = geometryHelper.converToPoint(geometryWkb);
        return new SinglePointLocation(id, point);
    }
}
