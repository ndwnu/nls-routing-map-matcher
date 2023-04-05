package nu.ndw.nls.routingmapmatcher.graphhopper.singlepoint;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;
import static nu.ndw.nls.routingmapmatcher.graphhopper.LinkWayIdEncodedValuesFactory.ID_NAME;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.MatchUtil.getQueryResults;
import static nu.ndw.nls.routingmapmatcher.graphhopper.util.PathUtil.determineEdgeDirection;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import java.util.Collections;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.SinglePointMapMatcher;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.MatchedPoint;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.singlepoint.SinglePointMatch.CandidateMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.EdgeIteratorTravelDirection;
import nu.ndw.nls.routingmapmatcher.graphhopper.model.MatchedQueryResult;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.BearingCalculator;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.FractionAndDistanceCalculator;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.util.GeometricShapeFactory;

public class GraphHopperSinglePointMapMatcher implements SinglePointMapMatcher {

    private static final int RADIUS_TO_DIAMETER = 2;

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int NUM_POINTS = 100;

    private static final GeometryFactory WGS84_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.WGS84_SRID);
    private static final GeometryFactory RD_NEW_GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(),
            GlobalConstants.RD_NEW_SRID);

    private final LocationIndexTree locationIndexTree;
    private final EdgeFilter edgeFilter;


    private final IsochroneService isochroneService;
    private final PointMatchingService pointMatchingService;
    private final CrsTransformer crsTransformer;

    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final NetworkGraphHopper network;

    public GraphHopperSinglePointMapMatcher(NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        this.network = network;
        this.locationIndexTree = network.getLocationIndex();
        this.edgeFilter = EdgeFilter.ALL_EDGES;
        BaseGraph baseGraph = network.getBaseGraph();
        EncodingManager encodingManager = network.getEncodingManager();
        BooleanEncodedValue accessEnc = encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR));
        DecimalEncodedValue speedEnc = encodingManager.getDecimalEncodedValue(VehicleSpeed.key(VEHICLE_CAR));
        Weighting weighting = new ShortestWeighting(accessEnc, speedEnc);
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
        FractionAndDistanceCalculator fractionAndDistanceCalculator = new FractionAndDistanceCalculator(
                geodeticCalculator);
        this.edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        this.isochroneService = new IsochroneService(encodingManager, baseGraph,
                edgeIteratorStateReverseExtractor,
                new IsochroneMatchMapper(new CrsTransformer(), encodingManager,
                        fractionAndDistanceCalculator,
                        edgeIteratorStateReverseExtractor),
                new ShortestPathTreeFactory(weighting),
                this.locationIndexTree
        );
        BearingCalculator bearingCalculator = new BearingCalculator(geodeticCalculator);
        this.pointMatchingService = new PointMatchingService(WGS84_GEOMETRY_FACTORY,
                bearingCalculator,
                fractionAndDistanceCalculator);
        this.crsTransformer = new CrsTransformer();
    }

    /*  Todo: this should be integrated into the existing match method.
         For now only the first match is returned with shortest distance.
     *  Proposal: introduce two smart enums. Filter and Sort in the SinglePointLocation
     *  with lambdas Filter.ALL (default) Filter.FIRST Sort.RELIABILITY (default) Sort.DISTANCE
     * */

    public SinglePointMatch match(SinglePointLocation singlePointLocation) {
        Preconditions.checkNotNull(singlePointLocation);
        Point inputPoint = singlePointLocation.getPoint();
        double inputRadius = singlePointLocation.getCutoffDistance();
        List<Snap> queryResults = findCandidates(inputPoint, inputRadius);
        Polygon circle = createCircle(inputPoint, RADIUS_TO_DIAMETER * inputRadius);
        List<MatchedPoint> matches = singlePointLocation
                .getMatchFilter()
                .getFilter()
                .filter(getMatchedPoints(singlePointLocation, queryResults, circle));
        if (matches.isEmpty()) {
            return createFailedMatchWithIsochrone(singlePointLocation);
        }
        List<CandidateMatch> candidateMatches = matches.stream()
                .map(matchedPoint -> {
                    List<IsochroneMatch> upstream =
                            singlePointLocation.getUpstreamIsochroneUnit() == null ? Collections.emptyList()
                                    : isochroneService
                                            .getUpstreamIsochroneMatches(matchedPoint
                                                    , singlePointLocation);
                    List<IsochroneMatch> downstream =
                            singlePointLocation.getDownstreamIsochroneUnit() == null ? Collections.emptyList()
                                    : isochroneService
                                            .getDownstreamIsochroneMatches(matchedPoint,
                                                    singlePointLocation);
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
                }).toList();

        return SinglePointMatch
                .builder()
                .id(singlePointLocation.getId())
                .reliability(candidateMatches.get(0).getReliability())
                .candidateMatches(candidateMatches)
                .status(MatchStatus.MATCH)
                .build();
    }

    private List<MatchedPoint> getMatchedPoints(SinglePointLocation singlePointLocation, List<Snap> queryResults,
            Polygon circle) {
        return queryResults.stream()
                .filter(qr -> intersects(circle, qr))
                .flatMap(qr -> calculateMatches(qr, circle, singlePointLocation)
                        .stream())
                .sorted(singlePointLocation.getMatchSort().getSort())
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

    private List<Snap> findCandidates(Point point, double radius) {
        return getQueryResults(network, point, radius, locationIndexTree, edgeFilter);
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
        EdgeIteratorTravelDirection travelDirection = determineEdgeDirection(queryResult, network.getEncodingManager());
        int matchedLinkId = queryResult.getClosestEdge().get(network.getEncodingManager()
                .getIntEncodedValue(ID_NAME));
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

    private SinglePointMatch createFailedMatchWithIsochrone(SinglePointLocation singlePointLocation) {
        return SinglePointMatch.builder()
                .id(singlePointLocation.getId())
                .candidateMatches(Lists.newArrayList())
                .reliability(0.0)
                .status(MatchStatus.NO_MATCH)
                .build();
    }
}
