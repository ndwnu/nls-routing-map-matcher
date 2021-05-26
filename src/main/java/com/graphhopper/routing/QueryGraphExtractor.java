package com.graphhopper.routing;

import com.graphhopper.routing.Path;
import com.graphhopper.routing.QueryGraph;
import nu.ndw.nls.routingmapmatcher.domain.exception.RoutingMapMatcherException;

/**
 * Extract a query graph from a path.
 * <p>
 * Uses the same package as {@link Path} to be able to access a protected variable of a path.
 */
public class QueryGraphExtractor {

  public QueryGraph extractQueryGraph(final Path path) {
    if (!(path.graph instanceof QueryGraph)) {
      throw new RoutingMapMatcherException("Expected graph of path to be of type QueryGraph");
    }
    return (QueryGraph) path.graph;
  }

}
