package nu.ndw.nls.routingmapmatcher.graphhopper.accessibility;

import com.google.common.base.Preconditions;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.BaseGraph;
import com.graphhopper.storage.EdgeIteratorStateReverseExtractor;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.PMap;
import java.util.Set;
import nu.ndw.nls.routingmapmatcher.domain.AccessibilityMap;
import nu.ndw.nls.routingmapmatcher.domain.model.IsochroneMatch;
import nu.ndw.nls.routingmapmatcher.domain.model.accessibility.AccessibilityRequest;
import nu.ndw.nls.routingmapmatcher.domain.model.routing.RoutingProfile;
import nu.ndw.nls.routingmapmatcher.graphhopper.NetworkGraphHopper;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleRestrictionsModel;
import nu.ndw.nls.routingmapmatcher.graphhopper.ev.VehicleType;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.IsochroneService;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.algorithm.ShortestPathTreeFactory;
import nu.ndw.nls.routingmapmatcher.graphhopper.isochrone.mappers.IsochroneMatchMapper;
import nu.ndw.nls.routingmapmatcher.graphhopper.util.CrsTransformer;


public class GraphHopperAccessibilityMap implements AccessibilityMap {

    private final IsochroneService isochroneService;
    private final NetworkGraphHopper network;

    public GraphHopperAccessibilityMap(NetworkGraphHopper network) {
        Preconditions.checkNotNull(network);
        this.network = network;
        LocationIndexTree locationIndexTree = network.getLocationIndex();
        BaseGraph baseGraph = network.getBaseGraph();
        EncodingManager encodingManager = network.getEncodingManager();
        Profile profile = VehicleType.CAR.createProfile(RoutingProfile.MOTOR_VEHICLE_CUSTOM.getLabel());
        EdgeIteratorStateReverseExtractor edgeIteratorStateReverseExtractor = new EdgeIteratorStateReverseExtractor();
        this.isochroneService = new IsochroneService(encodingManager, baseGraph, edgeIteratorStateReverseExtractor,
                new IsochroneMatchMapper(new CrsTransformer(), encodingManager, edgeIteratorStateReverseExtractor),
                new ShortestPathTreeFactory(network.createWeighting(profile, new PMap())), locationIndexTree);

    }

    @Override
    public Set<IsochroneMatch> getAccessibleRoadSections(AccessibilityRequest accessibilityRequest) {
        Profile profile = VehicleType.CAR.createProfile(RoutingProfile.MOTOR_VEHICLE_CUSTOM.getLabel());
        profile.setCustomModel(new VehicleRestrictionsModel(accessibilityRequest.vehicleProperties()));
        Weighting weighting = network.createWeighting(profile, new PMap());
        return isochroneService.getIsochroneMatchesByMunicipalityId(weighting, accessibilityRequest.startPoint(),
                accessibilityRequest.municipalityId());
    }
}
