package com.graphhopper.storage;

import com.graphhopper.storage.BaseGraph.EdgeIterable;
import com.graphhopper.storage.index.QueryResult;

public final class ReverseExtractor {

    private ReverseExtractor() {
    }

    public static boolean hasReversed(QueryResult q) {
        var edgeIterable = (EdgeIterable) q.getClosestEdge();
        return edgeIterable.reverse;
    }
}
