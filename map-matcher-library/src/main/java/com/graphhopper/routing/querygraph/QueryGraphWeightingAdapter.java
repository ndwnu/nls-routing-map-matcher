/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.graphhopper.routing.querygraph;

import static nu.ndw.nls.routingmapmatcher.network.model.Link.WAY_ID_KEY;

import com.carrotsearch.hppc.IntArrayList;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.QueryGraphWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.util.EdgeIteratorState;

/**
 * Whenever a {@link QueryGraph} is used for shortest path calculations including turn costs we need to wrap the
 * {@link Weighting} we want to use with this class. Otherwise turn costs at virtual nodes and/or including virtual
 * edges will not be calculated correctly.
 */
public class QueryGraphWeightingAdapter extends QueryGraphWeighting {

    private final boolean isSearchDirectionReversed;
    private final EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor;
    private final int firstVirtualEdgeId;
    private final int startSegmentLinkId;
    private final EncodingManager encodingManager;

    public QueryGraphWeightingAdapter(Weighting weighting, int firstVirtualNodeId, int firstVirtualEdgeId,
            IntArrayList closestEdges, EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor,
            boolean isSearchDirectionReversed, int startSegmentLinkId, EncodingManager encodingManager) {
        super(weighting, firstVirtualNodeId, firstVirtualEdgeId, closestEdges);
        this.isSearchDirectionReversed = isSearchDirectionReversed;
        this.edgeIteratorStateReverseExtractor = edgeIteratorStateReverseExtractor;
        this.firstVirtualEdgeId = firstVirtualEdgeId;
        this.startSegmentLinkId = startSegmentLinkId;
        this.encodingManager = encodingManager;
    }

    @Override
    public double calcEdgeWeight(EdgeIteratorState edgeState, boolean reverse) {
        // With bidirectional start segments, the search goes two ways for both down and upstream isochrones.
        // The branches that are starting in the wrong direction of travelling (as determined by the nearest
        // match) are filtered out.
        if (isVirtualEdge(edgeState.getEdge()) && searchDirectionIsCorrect(edgeState) && isStartSegment(edgeState)) {
            return Double.POSITIVE_INFINITY;
        }
        return super.calcEdgeWeight(edgeState, reverse);
    }

    private boolean isStartSegment(EdgeIteratorState edgeState) {
        return startSegmentLinkId == getLinkId(edgeState);
    }

    private boolean searchDirectionIsCorrect(EdgeIteratorState edgeState) {
        return edgeIteratorStateReverseExtractor.hasReversed(edgeState) != isSearchDirectionReversed;
    }

    private boolean isVirtualEdge(int edge) {
        return edge >= this.firstVirtualEdgeId;
    }

    private int getLinkId(EdgeIteratorState edge) {
        return edge.get(encodingManager.getIntEncodedValue(WAY_ID_KEY));
    }

    public static QueryGraphWeightingAdapter fromQueryGraph(Weighting baseWeighting, QueryGraph queryGraph,
            EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor, boolean reverseFlow, int matchedLinkId,
            EncodingManager encodingManager) {
        return new QueryGraphWeightingAdapter(baseWeighting,
                queryGraph.getBaseGraph().getNodes(),
                queryGraph.getBaseGraph().getEdges(),
                queryGraph
                        .getQueryOverlay()
                        .getClosestEdges(),
                edgeIteratorStateReverseExtractor,
                reverseFlow,
                matchedLinkId,
                encodingManager);
    }
}
