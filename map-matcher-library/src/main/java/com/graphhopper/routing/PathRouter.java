package com.graphhopper.routing;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.lm.LandmarkStorage;
import com.graphhopper.routing.querygraph.QueryGraph;
import com.graphhopper.routing.util.DirectedEdgeFilter;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.RoutingCHGraph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.StopWatch;
import com.graphhopper.util.TranslationMap;
import com.graphhopper.util.details.PathDetailsBuilderFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PathRouter extends Router {

    public PathRouter(BaseGraph graph, EncodingManager encodingManager,
            LocationIndex locationIndex,
            Map<String, Profile> profilesByName,
            PathDetailsBuilderFactory pathDetailsBuilderFactory,
            TranslationMap translationMap, RouterConfig routerConfig,
            WeightingFactory weightingFactory,
            Map<String, RoutingCHGraph> chGraphs,
            Map<String, LandmarkStorage> landmarks) {
        super(graph, encodingManager, locationIndex, profilesByName, pathDetailsBuilderFactory, translationMap,
                routerConfig, weightingFactory, chGraphs, landmarks);
    }

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
