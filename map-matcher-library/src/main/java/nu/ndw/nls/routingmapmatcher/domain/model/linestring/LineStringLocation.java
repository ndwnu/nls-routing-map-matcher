package nu.ndw.nls.routingmapmatcher.domain.model.linestring;

import com.google.common.base.MoreObjects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.LineString;

import java.util.Optional;

@RequiredArgsConstructor
@Getter
public class LineStringLocation {

  private final int id;
  private final Optional<Integer> locationIndex;
  private final Optional<Boolean> reversed;
  private final double lengthInMeters;
  private final LineString geometry;
  private final ReliabilityCalculationType reliabilityCalculationType;

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("locationIndex", locationIndex.orElse(null))
        .add("reversed", reversed.orElse(null))
        .add("lengthInMeters", lengthInMeters)
        .toString();
  }
}
