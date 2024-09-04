package nu.ndw.nls.routingmapmatcher.model.linestring;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.With;
import lombok.experimental.SuperBuilder;

@With
@Getter
@ToString
@SuperBuilder
@EqualsAndHashCode
@AllArgsConstructor
public class MatchedLink {

    private final int linkId;
    private final boolean reversed;
    private final double startFraction;
    private final double endFraction;
}
