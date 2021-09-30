package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.util.EdgeIteratorState;

import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.BaseLocation;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;
import nu.ndw.nls.routingmapmatcher.graphhopper.LinkFlagEncoder;

@RequiredArgsConstructor
public class IsochroneService {

    private final LinkFlagEncoder flagEncoder;
    private final Weighting weighting;

    public Set<Integer> getUpstreamLinkIds(final QueryGraph queryGraph, final BaseLocation location, final int nodeId) {
        return getIsochroneLinkIds(queryGraph, true, location.getUpstreamIsochrone(),
                location.getUpstreamIsochroneUnit(), nodeId);
    }

    public Set<Integer> getDownstreamLinkIds(final QueryGraph queryGraph, final BaseLocation location,
            final int nodeId) {
        return getIsochroneLinkIds(queryGraph, false, location.getDownstreamIsochrone(),
                location.getDownstreamIsochroneUnit(), nodeId);
    }

    private Set<Integer> getIsochroneLinkIds(final QueryGraph queryGraph, final boolean reverse,
            final double isochroneValue, final IsochroneUnit isochroneUnit, final int nodeId) {
        final Isochrone isochrone = new Isochrone(queryGraph, this.weighting, reverse);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
        final List<Isochrone.IsoLabel> labels = isochrone.search(nodeId);
        return labels.stream()
                .map(l -> queryGraph.getEdgeIteratorState(l.edge, l.adjNode))
                .map(EdgeIteratorState::getFlags)
                .map(flagEncoder::getId)
                .collect(Collectors.toSet());
    }
}
