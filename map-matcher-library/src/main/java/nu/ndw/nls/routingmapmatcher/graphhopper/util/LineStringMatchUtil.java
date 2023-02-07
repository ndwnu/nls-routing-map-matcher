package nu.ndw.nls.routingmapmatcher.graphhopper.util;

import com.google.common.collect.Lists;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.weighting.Weighting;
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

    public LineStringMatchUtil(final LinkFlagEncoder flagEncoder, final Weighting weighting) {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), GlobalConstants.WGS84_SRID);
        this.pathUtil = new PathUtil(geometryFactory);
        this.flagEncoder = flagEncoder;
        this.isochroneService = new IsochroneService(flagEncoder, weighting);
    }

    public LineStringMatch createMatch(final LineStringLocation lineStringLocation, final Path path,
            final QueryGraph queryGraph, final double reliability) {
        final List<EdgeIteratorState> edges = path.calcEdges();
        if (edges.isEmpty()) {
            throw new RoutingMapMatcherException("Unexpected: path has no edges");
        }
        final List<Integer> matchedLinkIds = pathUtil.determineMatchedLinkIds(flagEncoder, edges);

        final int startNode = edges.get(0).getBaseNode();
        final Set<Integer> upstreamLinkIds = lineStringLocation.getUpstreamIsochroneUnit() != null ?
                isochroneService.getUpstreamLinkIds(queryGraph, lineStringLocation, startNode) : null;
        final int endNode = edges.get(edges.size() - 1).getAdjNode();
        final Set<Integer> downstreamLinkIds = lineStringLocation.getDownstreamIsochroneUnit() != null ?
                isochroneService.getDownstreamLinkIds(queryGraph, lineStringLocation, endNode) : null;

        final double startLinkFraction = pathUtil.determineStartLinkFraction(edges.get(0), queryGraph);
        final double endLinkFraction = pathUtil.determineEndLinkFraction(edges.get(edges.size() - 1), queryGraph);
        final LineString lineString = pathUtil.createLineString(path.calcPoints());
        return new LineStringMatch(lineStringLocation.getId(), lineStringLocation.getLocationIndex(),
                lineStringLocation.isReversed(), matchedLinkIds, upstreamLinkIds, downstreamLinkIds, startLinkFraction,
                endLinkFraction, reliability, MatchStatus.MATCH, lineString);
    }

    public LineStringMatch createFailedMatch(final LineStringLocation lineStringLocation, final MatchStatus status) {
        final List<Integer> matchedLinkIds = Lists.newArrayList();
        final double startLinkFraction = 0.0;
        final double endLinkFraction = 0.0;
        final double reliability = 0.0;
        final LineString lineString = lineStringLocation.getGeometry();
        return new LineStringMatch(lineStringLocation.getId(), lineStringLocation.getLocationIndex(),
                lineStringLocation.isReversed(), matchedLinkIds, null, null, startLinkFraction, endLinkFraction,
                reliability, status, lineString);
    }
}