package nu.ndw.nls.routingmapmatcher.network.init.annotation.mappers;

import com.graphhopper.routing.ev.EdgeIntAccess;
import com.graphhopper.routing.ev.EncodedValueLookup;
import com.graphhopper.routing.ev.IntEncodedValue;
import nu.ndw.nls.routingmapmatcher.network.annotations.NetworkEncoded;
import nu.ndw.nls.routingmapmatcher.network.annotations.model.EncodedValueDto;

public class EncodedLongMapper<T extends NetworkEncoded> extends AbstractEncodedMapper<T, Long> {

    private static final String CODE_TOO_LARGE_MSG =
            "Cannot store %s: %d as it is too large (> %d). You can disable %s if you do not need it.";
    private final IntEncodedValue intEnc;
    private final String label;

    public EncodedLongMapper(EncodedValueLookup lookup, EncodedValueDto<T, Long> encodedValueDto) {
        super(encodedValueDto);
        this.intEnc = lookup.getIntEncodedValue(encodedValueDto.key());
        this.label = encodedValueDto.key();
    }

    @Override
    protected void set(boolean reverse, int edgeId, EdgeIntAccess edgeIntAccess, Long value) {
        if (value > intEnc.getMaxStorableInt()) {
            throw new IllegalArgumentException(CODE_TOO_LARGE_MSG.formatted(
                    label, value, intEnc.getMaxStorableInt(), intEnc.getName()));
        }
        intEnc.setInt(reverse, edgeId, edgeIntAccess, Math.toIntExact(value));
    }
}
