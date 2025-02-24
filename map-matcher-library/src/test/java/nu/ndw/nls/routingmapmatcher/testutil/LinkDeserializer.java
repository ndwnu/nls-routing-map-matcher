package nu.ndw.nls.routingmapmatcher.testutil;

import java.util.Optional;
import nu.ndw.nls.routingmapmatcher.testutil.TestNetworkProvider.TestLink;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.LineString;

public final class LinkDeserializer {

    private LinkDeserializer() {
        // Util class
    }

    public static TestLink deserialize(SimpleFeature feature) {
        int id = Integer.parseInt(feature.getID());
        Long linkIdReversed = Optional.ofNullable(feature.getProperty("linkIdReversed")).map(p -> (Integer) p.getValue())
                .map(Integer::longValue).orElse(null);
        int fromNodeId = (Integer) feature.getProperty("fromNodeId").getValue();
        int toNodeId = (Integer) feature.getProperty("toNodeId").getValue();
        double speedInKilometersPerHour = (Double) feature.getProperty("speedInKilometersPerHour").getValue();
        double reverseSpeedInKilometersPerHour = (Double) feature.getProperty("reverseSpeedInKilometersPerHour").getValue();
        double distanceInMeters = (Double) feature.getProperty("distanceInMeters").getValue();
        LineString lineString = (LineString) feature.getDefaultGeometry();

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
