package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import static nu.ndw.nls.routingmapmatcher.constants.GlobalConstants.VEHICLE_CAR;

import com.google.common.collect.Lists;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.ev.BooleanEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.VehicleAccess;
import com.graphhopper.routing.ev.VehicleSpeed;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.Direction;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

public class LineStringMatchUtil {

    public static final boolean INCLUDE_ELEVATION = false;
    private final PathUtil pathUtil;
    private final EncodingManager encodingManager;
    private final IsochroneService isochroneService;


    private LineStringMatchUtil(LocationIndexTree locationIndexTree, BaseGraph baseGraph,
            EncodingManager encodingManager) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(geometryFactory);
        this.encodingManager = encodingManager;
        BooleanEncodedValue accessEnc = encodingManager.getBooleanEncodedValue(VehicleAccess.key(VEHICLE_CAR));
        DecimalEncodedValue speedEnc = encodingManager.getDecimalEncodedValue(VehicleSpeed.key(VEHICLE_CAR));
        Weighting weighting = new ShortestWeighting(accessEnc, speedEnc);
        EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        IsochroneMatchMapper isochroneMatchMapper = new IsochroneMatchMapper(new CrsTransformer(), encodingManager,
                new FractionAndDistanceCalculator(new GeodeticCalculator()),
                edgeIteratorStateReverseExtractor);
        this.isochroneService = new IsochroneService(encodingManager, baseGraph,
                edgeIteratorStateReverseExtractor,
                isochroneMatchMapper, new ShortestPathTreeFactory(weighting), locationIndexTree);
    }

    public LineStringMatchUtil(NetworkGraphHopper networkGraphHopper) {
        this(networkGraphHopper.getLocationIndex(), networkGraphHopper.getBaseGraph(),
                networkGraphHopper.getEncodingManager());
    }

    public LineStringMatch createMatch(LineStringLocation lineStringLocation, Path path, QueryGraph queryGraph,
            double reliability) {
        List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        List<Integer> matchedLinkIds = pathUtil.determineMatchedLinkIds(encodingManager, edges);
        Point startPoint = edges.get(0)
                .fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION)
                .getStartPoint();
        Direction direction = lineStringLocation.isReversed() ? Direction.BACKWARD : Direction.FORWARD;
        List<IsochroneMatch> upstream = lineStringLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamIsochroneMatches(startPoint, direction, lineStringLocation) : null;
        Point endPoint = edges.get(edges.size() - 1).fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION)
                .getEndPoint();
        List<IsochroneMatch> downstream = lineStringLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamIsochroneMatches(endPoint, direction, lineStringLocation) : null;

        double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        LineString lineString = pathUtil.createLineString(path.calcPoints());
        return LineStringMatch.builder()
                .id(lineStringLocation.getId())
                .locationIndex(lineStringLocation.getLocationIndex())
                .reversed(lineStringLocation.isReversed())
                .matchedLinkIds(matchedLinkIds)
                .upstream(upstream)
                .downstream(downstream)
                .startLinkFraction(startLinkFraction)
                .endLinkFraction(endLinkFraction)
                .reliability(reliability)
                .status(MatchStatus.MATCH)
                .lineString(lineString)
                .build();
    }

    public LineStringMatch createFailedMatch(LineStringLocation lineStringLocation, MatchStatus status) {
        return LineStringMatch.builder()
                .id(lineStringLocation.getId())
                .locationIndex(lineStringLocation.getLocationIndex())
                .reversed(lineStringLocation.isReversed())
                .matchedLinkIds(Lists.newArrayList())
                .downstream(null)
                .upstream(null)
                .startLinkFraction(0.0)
                .endLinkFraction(0.0)
                .reliability(0.0)
                .status(status)
                .lineString(lineStringLocation.getGeometry())
                .build();
    }
}
