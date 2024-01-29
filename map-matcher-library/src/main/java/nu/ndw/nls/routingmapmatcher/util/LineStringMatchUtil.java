package nu.ndw.nls.routingmapmatcher.util;

import com.graphhopper.config.Profile;
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
import com.graphhopper.util.Helper;
import com.graphhopper.util.PathSimplification;
import com.graphhopper.util.PointList;
import com.graphhopper.util.RamerDouglasPeucker;
import java.util.List;
import nu.ndw.nls.routingmapmatcher.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.model.linestring.MatchedLink;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

public class LineStringMatchUtil {

    private static final boolean INCLUDE_ELEVATION = false;
    private static final int DECIMAL_PLACES = 3;
    private static final double MILLISECONDS_PER_SECOND = 1000.0;

    private final EncodingManager encodingManager;
    private final IsochroneService isochroneService;

    private LineStringMatchUtil(LocationIndexTree locationIndexTree, BaseGraph baseGraph,
            EncodingManager encodingManager, Profile profile) {
        this.encodingManager = encodingManager;
        BooleanEncodedValue accessEnc = encodingManager.getBooleanEncodedValue(VehicleAccess.key(profile.getVehicle()));
        DecimalEncodedValue speedEnc = encodingManager.getDecimalEncodedValue(VehicleSpeed.key(profile.getVehicle()));
        Weighting weighting = new ShortestWeighting(accessEnc, speedEnc);
        EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        IsochroneMatchMapper isochroneMatchMapper = new IsochroneMatchMapper(new CrsTransformer(), encodingManager,
                edgeIteratorStateReverseExtractor);
        this.isochroneService = new IsochroneService(encodingManager, baseGraph, edgeIteratorStateReverseExtractor,
                isochroneMatchMapper, new ShortestPathTreeFactory(weighting), locationIndexTree, profile);
    }

    public LineStringMatchUtil(NetworkGraphHopper networkGraphHopper, Profile profile) {
        this(networkGraphHopper.getLocationIndex(), networkGraphHopper.getBaseGraph(),
                networkGraphHopper.getEncodingManager(), profile);
    }

    public LineStringMatch createMatch(LineStringLocation lineStringLocation, Path path, QueryGraph queryGraph,
            double reliability) {
        List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        List<MatchedLink> matchedLinks = PathUtil.determineMatchedLinks(encodingManager, edges);

        Point startPoint = edges.get(0)
                .fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION)
                .getStartPoint();
        List<IsochroneMatch> upstream = lineStringLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamIsochroneMatches(startPoint, lineStringLocation.isReversed(),
                        lineStringLocation) : null;
        Point endPoint = edges.get(edges.size() - 1).fetchWayGeometry(FetchMode.ALL)
                .toLineString(INCLUDE_ELEVATION)
                .getEndPoint();
        List<IsochroneMatch> downstream = lineStringLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamIsochroneMatches(endPoint, lineStringLocation.isReversed(),
                        lineStringLocation) : null;

        double startLinkFraction = PathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        double endLinkFraction = PathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        PointList points = path.calcPoints();
        if (lineStringLocation.isSimplifyResponseGeometry()) {
            PathSimplification.simplify(points, List.of(), new RamerDouglasPeucker());
        }
        LineString lineString = points.toLineString(INCLUDE_ELEVATION);
        double distance = Helper.round(path.getDistance(), DECIMAL_PLACES);
        return LineStringMatch.builder()
                .id(lineStringLocation.getId())
                .locationIndex(lineStringLocation.getLocationIndex())
                .reversed(lineStringLocation.isReversed())
                .matchedLinks(matchedLinks)
                .upstream(upstream)
                .downstream(downstream)
                .startLinkFraction(startLinkFraction)
                .endLinkFraction(endLinkFraction)
                .reliability(reliability)
                .status(MatchStatus.MATCH)
                .lineString(lineString)
                // Viterbi does not provide a usable weight. StartToEnd uses distance as weight, so always using
                // distance changes nothing for StartToEnd and gives a usable (and comparable) value for Viterbi.
                .weight(distance)
                .duration(path.getTime() / MILLISECONDS_PER_SECOND)
                .distance(distance)
                .build();
    }

    public LineStringMatch createFailedMatch(LineStringLocation lineStringLocation, MatchStatus status) {
        return LineStringMatch.builder()
                .id(lineStringLocation.getId())
                .locationIndex(lineStringLocation.getLocationIndex())
                .reversed(lineStringLocation.isReversed())
                .matchedLinks(List.of())
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