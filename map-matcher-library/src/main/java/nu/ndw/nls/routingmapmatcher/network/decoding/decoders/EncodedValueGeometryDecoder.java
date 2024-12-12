package nu.ndw.nls.routingmapmatcher.network.decoding.decoders;

import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.FetchMode;
import com.graphhopper.util.PointList;
import lombok.RequiredArgsConstructor;
import nu.ndw.nls.routingmapmatcher.network.NetworkGraphHopper;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EncodedValueGeometryDecoder {

    public LineString decode(NetworkGraphHopper networkGraphHopper, long roadSectionId) {
        Integer i = networkGraphHopper.getEdgeMap().get(roadSectionId);
        EdgeIteratorState edgeIteratorState = networkGraphHopper.getBaseGraph().getEdgeIteratorStateForKey(i);

        PointList geometryPointList = edgeIteratorState.fetchWayGeometry(FetchMode.ALL);

        return geometryPointList.toLineString(geometryPointList.is3D());
    }

}
