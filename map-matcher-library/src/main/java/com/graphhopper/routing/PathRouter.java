package com.graphhopper.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.DirectedEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetailsBuilderFactory;
import java.util.List;
import java.util.Map;

/**
 * Extension of class in order to implement calcPaths api.
 *
 * @see <a
 * href="https://github.com/graphhopper/graphhopper/blob/0.12/core/src/main/java/com/graphhopper/GraphHopper.java#L929">original
 * routing api</a>
 */
public class PathRouter extends Router {

    public PathRouter(BaseGraph graph, EncodingManager encodingManager,
            LocationIndex locationIndex,
            Map<String,Profile> profilesByName,
            PathDetailsBuilderFactory pathDetailsBuilderFactory,
            TranslationMap translationMap, RouterConfig routerConfig,
            WeightingFactory weightingFactory,
            Map<String, RoutingCHGraph> chGraphs,
            Map<String, LandmarkStorage> landmarks) {
        super(graph, encodingManager, locationIndex, profilesByName, pathDetailsBuilderFactory, translationMap,
                routerConfig, weightingFactory, chGraphs, landmarks);
    }

    /**
     * Part taken from router.routeVia in order to get access to the edges and id's via the paths This is used to get
     * the link ids and determine the start and end fractions. In the previous version (0.12) of graphhopper this was
     * part of the GH routing api.
     *
     * @param request the gh routing request
     * @return a list of path objects
     * @see <a
     * href="https://github.com/graphhopper/graphhopper/blob/0.12/core/src/main/java/com/graphhopper/GraphHopper.java#L929">original
     * routing api </a>
     */
    public List<Path> calcPaths(GHRequest request) {
        Solver solver = createSolver(request);
        solver.checkRequest();
        solver.profile = solver.getProfile();
        solver.checkProfileCompatibility();
        solver.weighting = solver.createWeighting();
        DirectedEdgeFilter directedEdgeFilter = solver.createDirectedEdgeFilter();
        List<Snap> snaps = ViaRouting.lookup(encodingManager, request.getPoints(), solver.createSnapFilter(),
                locationIndex,
                request.getSnapPreventions(), request.getPointHints(), directedEdgeFilter, request.getHeadings());
        // (base) query graph used to resolve headings, curbsides etc. this is not necessarily the same thing as
        // the (possibly implementation specific) query graph used by PathCalculator
        QueryGraph queryGraph = QueryGraph.create(graph, snaps);
        PathCalculator pathCalculator = solver.createPathCalculator(queryGraph);
        boolean passThrough = false;
        boolean forceCurbsides = false;
        ViaRouting.Result result = ViaRouting.calcPaths(request.getPoints(), queryGraph, snaps, directedEdgeFilter,
                pathCalculator, request.getCurbsides(), forceCurbsides, request.getHeadings(), passThrough);
        if (request.getPoints().size() != result.paths.size() + 1) {
            throw new RuntimeException(
                    "There should be exactly one more point than paths. points:" + request.getPoints().size()
                            + ", paths:" + result.paths.size());
        }

        return result.paths;
    }
}
