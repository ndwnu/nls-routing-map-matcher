package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import com.google.common.collect.Lists;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;
import java.util.List;
import java.util.Set;
import nu.ndw.nls.routingmapmatcher.constants.GlobalConstants;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;
import nu.ndw.nls.routingmapmatcher.domain.model.MatchStatus;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.linestring.LineStringMatch;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;

public class LineStringMatchUtil {

    private final PathUtil pathUtil;
    private final LinkFlagEncoder flagEncoder;
    private final IsochroneService isochroneService;

    public LineStringMatchUtil(LinkFlagEncoder flagEncoder, Weighting weighting) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(geometryFactory);
        this.flagEncoder = flagEncoder;
        this.isochroneService = new IsochroneService(flagEncoder, weighting, new EdgeIteratorStateReverseExtractor(),
                null);
    }

    public LineStringMatch createMatch(LineStringLocation lineStringLocation, Path path, QueryGraph queryGraph,
            double reliability) {
        List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        List<Integer> matchedLinkIds = pathUtil.determineMatchedLinkIds(flagEncoder, edges);

        int startNode = edges.get(0).getBaseNode();
        Set<Integer> upstreamLinkIds = lineStringLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamLinkIds(queryGraph, lineStringLocation, startNode) : null;
        int endNode = edges.get(edges.size() - 1).getAdjNode();
        Set<Integer> downstreamLinkIds = lineStringLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamLinkIds(queryGraph, lineStringLocation, endNode) : null;

        double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        LineString lineString = pathUtil.createLineString(path.calcPoints());
        return LineStringMatch.builder()
                .id(lineStringLocation.getId())
                .locationIndex(lineStringLocation.getLocationIndex())
                .reversed(lineStringLocation.isReversed())
                .matchedLinkIds(matchedLinkIds)
                .upstreamLinkIds(upstreamLinkIds)
                .downstreamLinkIds(downstreamLinkIds)
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
                .upstreamLinkIds(null)
                .downstreamLinkIds(null)
                .startLinkFraction(0.0)
                .endLinkFraction(0.0)
                .reliability(0.0)
                .status(status)
                .lineString(lineStringLocation.getGeometry())
                .build();
    }
}
