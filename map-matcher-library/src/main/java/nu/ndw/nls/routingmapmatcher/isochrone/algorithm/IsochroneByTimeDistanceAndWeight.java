package nu.ndw.nls.routingmapmatcher.isochrone.algorithm;


import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;

/**
 * This class is a fork of the com.graphhopper.isochrone.algorithm. ShortestPathTree class. The inclusion logic is
 * different from the original class because the original current implementation differed from the previous
 * implementation in v 0.12. The previous implementation in v 0.12 included IsoLabels which had a partial limit (ie a
 * road-segment of 100 meters which still could be travelled for 50 meters until reaching the limit was included). The
 * current implementation in the graphhopper Library did not include those partial road-segments leading to unwanted
 * results for nls requirements. This class fixes this by replacing the original check for inclusion
 * getExploreValue(label) <= limit with (this.limit - getExploreValue(isoLabel.parent)) > 0.
 */
public class IsochroneByTimeDistanceAndWeight extends AbstractShortestPathTree {

    enum ExploreType {TIME, DISTANCE, WEIGHT}

    private double limit = -1;
    private ExploreType exploreType = ExploreType.TIME;

    public IsochroneByTimeDistanceAndWeight(Graph g, Weighting weighting, boolean upstream, boolean startingDirectionReversed,
            TraversalMode traversalMode) {
        super(g, weighting, upstream, startingDirectionReversed, traversalMode);

    }

    /**
     * Time limit in milliseconds
     */
    public void setTimeLimit(double limit) {
        exploreType = ExploreType.TIME;
        this.limit = limit;
    }

    /**
     * Distance limit in meter
     */
    public void setDistanceLimit(double limit) {
        exploreType = ExploreType.DISTANCE;
        this.limit = limit;
    }

    public void setWeightLimit(double limit) {
        exploreType = ExploreType.WEIGHT;
        this.limit = limit;
    }

    private double getExploreValue(IsoLabel label) {
        if (exploreType == ExploreType.TIME) {
            return label.getTime();
        }
        if (exploreType == ExploreType.WEIGHT) {
            return label.getWeight();
        }
        return label.getDistance();
    }

    @Override
    protected boolean isInLimit(IsoLabel isoLabel) {
        return (this.limit - getExploreValue(isoLabel.getParent())) > 0;
    }

}
