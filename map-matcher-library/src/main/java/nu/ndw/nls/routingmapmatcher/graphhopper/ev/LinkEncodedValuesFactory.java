package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueFactory;
import com.graphhopper.routing.ev.MaxAxleLoad;
import com.graphhopper.routing.ev.MaxHeight;
import com.graphhopper.routing.ev.MaxLength;
import com.graphhopper.routing.ev.MaxWeight;
import com.graphhopper.routing.ev.MaxWidth;
import com.graphhopper.util.PMap;

public class LinkEncodedValuesFactory implements EncodedValueFactory {

    @Override
    public EncodedValue create(String name, PMap properties) {
        return switch (EncodedTag.withKey(name)) {
            case WAY_ID -> WayId.create();
            case MAX_WEIGHT -> MaxWeight.create();
            case MAX_WIDTH -> MaxWidth.create();
            case MAX_LENGTH -> MaxLength.create();
            case MAX_AXLE_LOAD -> MaxAxleLoad.create();
            case MAX_HEIGHT -> MaxHeight.create();
            case MUNICIPALITY_CODE -> MunicipalityCode.create();
            case HGV_ACCESSIBLE -> HgvAccessible.create();
        };
    }

}
