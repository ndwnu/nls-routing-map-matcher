package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.SneakyThrows;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LineStringLocationDeserializer;
import nu.ndw.nls.routingmapmatcher.graphhopper.viterbi.LinkDeserializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.wololo.geojson.Feature;
import org.wololo.geojson.GeoJSON;
import org.wololo.jts2geojson.GeoJSONWriter;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateGeoJsonFromLinks {
    private ObjectMapper mapper;

    @SneakyThrows
    @Test
    @Disabled
    void createGeoJson() {
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });

        List<Feature> featuresJson = new ArrayList<>();
        links.forEach(l -> {
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("id", l.getId());
            properties.put("fromNodeId", l.getFromNodeId());
            properties.put("toNodeId", l.getToNodeId());
            properties.put("speedInKilometersPerHour", l.getSpeedInKilometersPerHour());
            properties.put("reverseSpeedInKilometersPerHour", l.getReverseSpeedInKilometersPerHour());
            properties.put("distanceInMeters", l.getDistanceInMeters());
            GeoJSONWriter writer = new GeoJSONWriter();
            var geometry = writer.write(l.getGeometry());

            featuresJson.add(new Feature(l.getId(), geometry, properties));
        });

        GeoJSONWriter writer = new GeoJSONWriter();
        GeoJSON json = writer.write(featuresJson);
        FileUtils.writeStringToFile(new File("/tmp/network.geojson"), json.toString(), Charset.defaultCharset().name());
    }

    @SneakyThrows
    @Test
    @Disabled
    // TODO Work in progress
    void testIntersection() {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        var shapeFactory =  new GeometricShapeFactory(gf);
        shapeFactory.setCentre(new Coordinate(5.426747, 52.176663));
        shapeFactory.setNumPoints(100); // adjustable
        // Length in meters of 1° of latitude = always 111.32 km
        shapeFactory.setWidth(100d/111320d);
        // Length in meters of 1° of longitude = 40075 km * cos( latitude ) / 360
        shapeFactory.setHeight(100d / (40075000 * Math.cos(Math.toRadians(5.426747)) / 360));

        Polygon circleA = shapeFactory.createEllipse();
//        Point p = gf.createPoint(new Coordinate(5.426747, 52.176663));
//        //configure the circle as appropriate
//        Polygon circleA =  (Polygon) p.buffer(0.001);
        GeoJSONWriter writerCircle = new GeoJSONWriter();
        var circleGeometry = writerCircle.write(circleA);
        var feature = new Feature("circle", circleGeometry, new HashMap<String, Object>());

        GeoJSONWriter circleGeoJson = new GeoJSONWriter();
        GeoJSON circleJson = circleGeoJson.write(List.of(feature));
        FileUtils.writeStringToFile(new File("/tmp/circle.geojson"), circleJson.toString(), Charset.defaultCharset().name());
        //var results = circleA.intersection(l.getGeometry());
        String linksJson = IOUtils.toString(getClass().getResourceAsStream("/test-data/links.json"));
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Link.class, new LinkDeserializer());
        module.addDeserializer(LineStringLocation.class, new LineStringLocationDeserializer());
        mapper.registerModule(module);
        List<Link> links = mapper.readValue(linksJson, new TypeReference<>() {
        });

        var geoms = links.stream()
                .filter(l -> circleA.intersects(l.getGeometry()))
                .map(l -> {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("id", l.getId());
                    properties.put("fromNodeId", l.getFromNodeId());
                    properties.put("toNodeId", l.getToNodeId());
                    properties.put("speedInKilometersPerHour", l.getSpeedInKilometersPerHour());
                    properties.put("reverseSpeedInKilometersPerHour", l.getReverseSpeedInKilometersPerHour());
                    properties.put("distanceInMeters", l.getDistanceInMeters());
                    GeoJSONWriter writer = new GeoJSONWriter();
                    var geometry = writer.write(circleA.intersection(l.getGeometry()));
                    return new Feature(l.getId(), geometry, properties);
                }).collect(Collectors.toList());

        GeoJSONWriter writer = new GeoJSONWriter();
        GeoJSON json = writer.write(geoms);
        FileUtils.writeStringToFile(new File("/tmp/network-cropped.geojson"), json.toString(), Charset.defaultCharset().name());

    }
}
