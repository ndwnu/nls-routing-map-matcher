package com.graphhopper.routing;

import com.graphhopper.routing.querygraph.QueryGraph;
import nu.ndw.nls.routingmapmatcher.exception.RoutingMapMatcherException;

/**
 * Extract a query graph from a path.
 * <p>
 * Uses the same package as {@link Path} to be able to access a protected variable of a path.
 */
public final class QueryGraphExtractor {

    private QueryGraphExtractor() {
        // Util class
    }

    public static QueryGraph extractQueryGraph(Path path) {
        if (!(path.graph instanceof QueryGraph queryGraph)) {
            throw new RoutingMapMatcherException("Expected graph of path to be of type QueryGraph");
        }
        return queryGraph;
    }
}
