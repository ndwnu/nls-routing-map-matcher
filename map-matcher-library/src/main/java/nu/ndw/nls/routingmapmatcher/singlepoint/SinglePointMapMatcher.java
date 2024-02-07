package nu.ndw.nls.routingmapmatcher.singlepoint;

import static nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchFilter.ALL;
import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;
import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.RD_NEW_GEOMETRY_FACTORY;
import static nu.ndw.nls.routingmapmatcher.util.GeometryConstants.WGS84_GEOMETRY_FACTORY;
import static nu.ndw.nls.routingmapmatcher.util.MatchUtil.getQueryResults;
import static nu.ndw.nls.routingmapmatcher.util.PathUtil.determineEdgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FiniteWeightFilter;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.domain.MapMatcher;
import nu.ndw.nls.routingmapmatcher.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.util.CrsTransformer;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;

public class SinglePointMapMatcher implements MapMatcher<SinglePointLocation, SinglePointMatch> {

    private static final int RADIUS_TO_DIAMETER = 2;

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int NUM_POINTS = 100;
    private static final double DISTANCE_THRESHOLD = 0.1;
    private static final double RELIABILITY_THRESHOLD = 0.5;

    private final LocationIndexTree locationIndexTree;
    private final IsochroneService isochroneService;
    private final PointMatchingService pointMatchingService;
    private final CrsTransformer crsTransformer;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final NetworkGraphHopper network;
    private final Profile profile;

    public SinglePointMapMatcher(NetworkGraphHopper network, String profileName) {
        this.network = Preconditions.checkNotNull(network);
        this.profile = Preconditions.checkNotNull(network.getProfile(profileName));
        this.locationIndexTree = network.getLocationIndex();
        BaseGraph baseGraph = network.getBaseGraph();
        EncodingManager encodingManager = network.getEncodingManager();
        BooleanEncodedValue accessEnc = encodingManager.getBooleanEncodedValue(VehicleAccess.key(profile.getVehicle()));
        DecimalEncodedValue speedEnc = encodingManager.getDecimalEncodedValue(VehicleSpeed.key(profile.getVehicle()));
        Weighting weighting = new ShortestWeighting(accessEnc, speedEnc);
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        this.edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        this.isochroneService = new IsochroneService(encodingManager, baseGraph, edgeIteratorStateReverseExtractor,
                new IsochroneMatchMapper(new CrsTransformer(), encodingManager, edgeIteratorStateReverseExtractor),
                new ShortestPathTreeFactory(weighting), this.locationIndexTree, profile);
        BearingCalculator bearingCalculator = new BearingCalculator(geodeticCalculator);
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY, bearingCalculator);
        this.crsTransformer = new CrsTransformer();
    }

    public SinglePointMatch match(SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        Weighting matchWeighting = network.createWeighting(profile, new PMap());
        Point inputPoint = singlePointLocation.getPoint();
        double inputRadius = singlePointLocation.getCutoffDistance();
        List<Snap> queryResults = getQueryResults(network, inputPoint, inputRadius, locationIndexTree,
                new FiniteWeightFilter(matchWeighting));
        Polygon circle = createCircle(inputPoint, RADIUS_TO_DIAMETER * inputRadius);
        List<MatchedPoint> matches = getMatchedPoints(singlePointLocation, queryResults, circle);
        if (matches.isEmpty()) {
            return createFailedMatch(singlePointLocation);
        }
        List<CandidateMatch> candidateMatches = matches.stream()
                .map(matchedPoint -> mapToCandidateMatch(singlePointLocation, matchedPoint))
                .toList();

        return SinglePointMatch
                .builder()
                .id(singlePointLocation.getId())
                .reliability(candidateMatches.get(0).getReliability())
                .candidateMatches(candidateMatches)
                .status(MatchStatus.MATCH)
                .build();
    }

    private CandidateMatch mapToCandidateMatch(SinglePointLocation singlePointLocation, MatchedPoint matchedPoint) {
        List<IsochroneMatch> upstream = singlePointLocation.getUpstreamIsochroneUnit() == null ? null
                : isochroneService.getUpstreamIsochroneMatches(matchedPoint.getSnappedPoint(),
                        matchedPoint.isReversed(), singlePointLocation);
        List<IsochroneMatch> downstream = singlePointLocation.getDownstreamIsochroneUnit() == null ? null
                : isochroneService.getDownstreamIsochroneMatches(matchedPoint.getSnappedPoint(),
                        matchedPoint.isReversed(), singlePointLocation);

        return CandidateMatch
                .builder()
                .matchedLinkId(matchedPoint.getMatchedLinkId())
                .reversed(matchedPoint.isReversed())
                .upstream(upstream)
                .downstream(downstream)
                .snappedPoint(matchedPoint.getSnappedPoint())
                .fraction(matchedPoint.getFraction())
                .distance(matchedPoint.getDistance())
                .bearing(matchedPoint.getBearing())
                .reliability(matchedPoint.getReliability())
                .build();
    }

    private List<MatchedPoint> getMatchedPoints(SinglePointLocation singlePointLocation, List<Snap> queryResults,
            Polygon circle) {
        List<MatchedPoint> sorted = queryResults.stream()
                .filter(qr -> intersects(circle, qr))
                .flatMap(qr -> calculateMatches(qr, circle, singlePointLocation)
                        .stream())
                .sorted(singlePointLocation.getMatchSort().getSort())
                .toList();
        if (sorted.isEmpty()) {
            return sorted;
        }
        if (singlePointLocation.getMatchFilter() == ALL) {
            return sorted;
        } else {
            return switch (singlePointLocation.getMatchSort()) {
                case HIGHEST_RELIABILITY -> getMostReliable(sorted);
                case SHORTEST_DISTANCE -> getShortest(sorted);
            };
        }
    }

    private List<MatchedPoint> getShortest(List<MatchedPoint> sorted) {
        double cutoffValue = sorted.get(0).getDistance() + DISTANCE_THRESHOLD;
        return sorted
                .stream()
                .filter(matchedPoint -> matchedPoint.getDistance() < cutoffValue)
                .toList();
    }

    private List<MatchedPoint> getMostReliable(List<MatchedPoint> sorted) {
        double cutoffValue = sorted.get(0).getReliability() - RELIABILITY_THRESHOLD;
        return sorted
                .stream()
                .filter(matchedPoint -> matchedPoint.getReliability() > cutoffValue)
                .toList();
    }


    private boolean intersects(Polygon circle, Snap queryResult) {
        PointList pl = queryResult.getClosestEdge().fetchWayGeometry(FetchMode.ALL);
        return circle.intersects(pl.toLineString(INCLUDE_ELEVATION));
    }

    private Polygon createCircle(Point pointWgs84, double diameterInMeters) {
        var shapeFactory = new GeometricShapeFactory(RD_NEW_GEOMETRY_FACTORY);
        Point pointRd = (Point) crsTransformer.transformFromWgs84ToRdNew(pointWgs84);
        shapeFactory.setCentre(new Coordinate(pointRd.getX(), pointRd.getY()));
        shapeFactory.setNumPoints(NUM_POINTS);
        shapeFactory.setWidth(diameterInMeters);
        shapeFactory.setHeight(diameterInMeters);
        Polygon ellipseRd = shapeFactory.createEllipse();
        return (Polygon) crsTransformer.transformFromRdNewToWgs84(ellipseRd);
    }

    private List<MatchedPoint> calculateMatches(Snap queryResult, Polygon circle,
            SinglePointLocation singlePointLocation) {
        LineString wayGeometry = queryResult.getClosestEdge()
                .fetchWayGeometry(FetchMode.ALL)
                .toLineString(false);
        /*
           The geometry direction of the edge iterator wayGeometry does not necessarily reflect the direction of a
           street or the original encoded geometry direction. It is just the direction of the exploration of the graph.
           GraphHopper sometimes reverses the geometry direction with respect to the original direction. To fix this,
           an internal attribute of the edge iterator state is used, indicating it has done so or not.
        */
        LineString originalGeometry =
                edgeIteratorStateReverseExtractor.hasReversed(queryResult.getClosestEdge()) ? wayGeometry.reverse()
                        : wayGeometry;
        Geometry cutoffGeometry = circle.intersection(originalGeometry);
        EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(queryResult, network.getEncodingManager(),
                profile.getVehicle());
        int matchedLinkId = queryResult.getClosestEdge().get(network.getEncodingManager()
                .getIntEncodedValue(WAY_ID_KEY));
        var matchedQueryResult = MatchedQueryResult.builder()
                .matchedLinkId(matchedLinkId)
                .inputPoint(singlePointLocation.getPoint())
                .cutoffDistance(singlePointLocation.getCutoffDistance())
                .bearingFilter(singlePointLocation.getBearingFilter())
                .travelDirection(travelDirection)
                .originalGeometry(originalGeometry)
                .cutoffGeometry(cutoffGeometry)
                .build();

        return pointMatchingService.calculateMatches(matchedQueryResult);

    }

    private SinglePointMatch createFailedMatch(SinglePointLocation singlePointLocation) {
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }
}
