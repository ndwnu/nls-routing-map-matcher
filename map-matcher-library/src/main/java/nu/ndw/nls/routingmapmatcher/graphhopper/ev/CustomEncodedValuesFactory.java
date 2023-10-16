package nu.ndw.nls.routingmapmatcher.graphhopper.ev;

import com.graphhopper.routing.ev.DecimalEncodedValue;
import com.graphhopper.routing.ev.DecimalEncodedValueImpl;
import com.graphhopper.routing.ev.EncodedValue;
import com.graphhopper.routing.ev.EncodedValueFactory;
import com.graphhopper.routing.ev.IntEncodedValue;
import com.graphhopper.routing.ev.IntEncodedValueImpl;
import com.graphhopper.routing.ev.SimpleBooleanEncodedValue;
import com.graphhopper.routing.ev.StringEncodedValue;
import com.graphhopper.util.PMap;

public class CustomEncodedValuesFactory implements EncodedValueFactory {

    private static final double DECIMAL_FACTOR = 0.1;

    @Override
    public EncodedValue create(String name, PMap properties) {
        EncodedTag encodedTag = EncodedTag.withKey(name);
        return switch (encodedTag.getEncodingType()) {
            case STRING -> this.getStringEncodedValue(encodedTag);
            case INT -> this.getIntEncodedValue(encodedTag);
            case DECIMAL -> this.getDecimalEncodedValue(encodedTag);
            case BOOLEAN -> this.getBooleanEncodedValue(encodedTag);
        };

    }

    private StringEncodedValue getStringEncodedValue(EncodedTag encodedTag) {
        return new StringEncodedValue(encodedTag.getKey(), 1, encodedTag.isSeparateValuesPerDirection());
    }

    private IntEncodedValue getIntEncodedValue(EncodedTag encodedTag) {
        return new IntEncodedValueImpl(encodedTag.getKey(), encodedTag.getBits(),
                encodedTag.isSeparateValuesPerDirection());
    }

    private DecimalEncodedValue getDecimalEncodedValue(EncodedTag encodedTag) {
        String name = encodedTag.getKey();
        Integer bits = encodedTag.getBits();
        boolean separateValuesPerDirection = encodedTag.isSeparateValuesPerDirection();
        return new DecimalEncodedValueImpl(name, bits, 0.0, DECIMAL_FACTOR, false, separateValuesPerDirection, true);
    }

    private SimpleBooleanEncodedValue getBooleanEncodedValue(EncodedTag encodedTag) {
        return new SimpleBooleanEncodedValue(encodedTag.getKey(), encodedTag.isSeparateValuesPerDirection());
    }

}
