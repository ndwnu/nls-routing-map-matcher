package nu.ndw.nls.routingmapmatcher.testutil;

import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.ReliabilityCalculationType;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.LineString;

public final class LineStringLocationDeserializer {

    private LineStringLocationDeserializer() {
        // Util class
    }

    public static LineStringLocation deserialize(SimpleFeature feature) {
        int id = Integer.parseInt(feature.getID());
        int locationIndex = (Integer) feature.getProperty("locationIndex").getValue();
        boolean reversed = (Boolean) feature.getProperty("reversed").getValue();
        ReliabilityCalculationType reliabilityCalculationType = ReliabilityCalculationType
                .valueOf((String) feature.getProperty("reliabilityCalculationType").getValue());
        double upstreamIsochrone = Optional.ofNullable(feature.getProperty("upstreamIsochrone"))
                .map(p -> (Double) p.getValue()).orElse(0.0);
        IsochroneUnit upstreamIsochroneUnit = Optional.ofNullable(feature.getProperty("upstreamIsochroneUnit"))
                .map(p -> (String) p.getValue()).map(IsochroneUnit::valueOf).orElse(null);
        double downstreamIsochrone = Optional.ofNullable(feature.getProperty("downstreamIsochrone"))
                .map(p -> (Double) p.getValue()).orElse(0.0);
        IsochroneUnit downstreamIsochroneUnit = Optional.ofNullable(feature.getProperty("downstreamIsochroneUnit"))
                .map(p -> (String) p.getValue()).map(IsochroneUnit::valueOf).orElse(null);
        LineString lineString = (LineString) feature.getDefaultGeometry();

        return LineStringLocation.builder()
                .id(id)
                .locationIndex(locationIndex)
                .reversed(reversed)
                .geometry(lineString)
                .reliabilityCalculationType(reliabilityCalculationType)
                .upstreamIsochrone(upstreamIsochrone)
                .upstreamIsochroneUnit(upstreamIsochroneUnit)
                .downstreamIsochrone(downstreamIsochrone)
                .downstreamIsochroneUnit(downstreamIsochroneUnit)
                .build();
    }
}
