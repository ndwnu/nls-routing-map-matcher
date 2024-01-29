package nu.ndw.nls.routingmapmatcher.util;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;

public class CrsTransformer {

    private final MathTransform transformFromWgs84ToRdNew;
    private final MathTransform transformFromRdNewToWgs84;

    public CrsTransformer() {
        try {
            // Longitude first prevents swapped coordinates, see
            // https://gis.stackexchange.com/questions/433425/geotools-transform-to-new-coordinate-system-not-working
            boolean longitudeFirst = true;
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:" + GeometryConstants.WGS84_SRID, longitudeFirst);
            CoordinateReferenceSystem rdNew = CRS.decode("EPSG:" + GeometryConstants.RD_NEW_SRID, longitudeFirst);
            transformFromWgs84ToRdNew = CRS.findMathTransform(wgs84, rdNew);
            transformFromRdNewToWgs84 = CRS.findMathTransform(rdNew, wgs84);
        } catch (FactoryException e) {
            throw new IllegalStateException("Failed to initialize coordinate reference systems", e);
        }
    }

    public Geometry transformFromWgs84ToRdNew(Geometry geometry) {
        try {
            return JTS.transform(geometry, transformFromWgs84ToRdNew);
        } catch (TransformException e) {
            throw new IllegalStateException("Failed to transform from WGS84 to RD New", e);
        }
    }

    public Geometry transformFromRdNewToWgs84(Geometry geometry) {
        try {
            return JTS.transform(geometry, transformFromRdNewToWgs84);
        } catch (TransformException e) {
            throw new IllegalStateException("Failed to transform from RD New to WGS84", e);
        }
    }
}
