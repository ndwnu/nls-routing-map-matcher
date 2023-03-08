package nu.ndw.nls.routingmapmatcher.graphhopper.isochrone;

import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.weighting.Weighting;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneUnit;

@RequiredArgsConstructor
public class IsochroneFactory {
    private final Weighting weighting;
    public Isochrone createIsochrone(QueryGraph queryGraph, double isochroneValue, IsochroneUnit isochroneUnit,
            boolean reverseFlow) {
        Isochrone isochrone = new Isochrone(queryGraph, this.weighting, reverseFlow);
        if (isochroneUnit == IsochroneUnit.METERS) {
            isochrone.setDistanceLimit(isochroneValue);
        } else if (isochroneUnit == IsochroneUnit.SECONDS) {
            isochrone.setTimeLimit(isochroneValue);
        } else {
            throw new IllegalArgumentException("Unexpected isochrone unit");
        }
        return isochrone;
    }
}
