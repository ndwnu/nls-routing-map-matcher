package nu.ndw.nls.routingmapmatcher.graphhopper;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.profiles.EncodedValue;
import com.graphhopper.routing.profiles.FactorizedDecimalEncodedValue;
import com.graphhopper.routing.profiles.SimpleIntEncodedValue;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.IntsRef;
import nu.ndw.nls.routingmapmatcher.domain.model.Link;

import java.util.List;

public class LinkFlagEncoder extends AbstractFlagEncoder {

  public static final String NAME = "links";

  private final boolean speedInTwoDirections;
  private final double maximumSpeed;

  private SimpleIntEncodedValue idEncoder;

  public LinkFlagEncoder() {
    this(24, 0.0001, 1500, true, 0);
  }

  public LinkFlagEncoder(final int speedBits, final double speedFactor, final double maximumSpeed,
      final boolean speedInTwoDirections, final int maxTurnCosts) {
    super(speedBits, speedFactor, maxTurnCosts);
    final double maximumEncodedSpeed = ((1L << speedBits) - 1) * speedFactor;
    this.maximumSpeed = Math.max(maximumSpeed, maximumEncodedSpeed);
    this.speedInTwoDirections = speedInTwoDirections;
  }

  @Override
  public int getVersion() {
    return 1;
  }

  @Override
  public long handleRelationTags(final long oldRelationFlags, final ReaderRelation relation) {
      return oldRelationFlags;
  }

  @Override
  public EncodingManager.Access getAccess(final ReaderWay way) {
    final Link link = castToLink(way);
    final boolean access = link.getSpeedInKilometersPerHour() > 0.0 ||
        link.getReverseSpeedInKilometersPerHour() > 0.0;
    return access ? EncodingManager.Access.WAY : EncodingManager.Access.CAN_SKIP;
  }

  private Link castToLink(final ReaderWay way) {
    final Link link = (Link) way;
    if (link == null) {
      throw new IllegalStateException("Only LinkDto's are supported by this flag encoder");
    }
    return link;
  }

  @Override
  public void createEncodedValues(final List<EncodedValue> registerNewEncodedValue, final String prefix,
      final int index) {
      super.createEncodedValues(registerNewEncodedValue, prefix, index);

      speedEncoder = new FactorizedDecimalEncodedValue(prefix + "average_speed", speedBits, speedFactor,
          speedInTwoDirections);
      registerNewEncodedValue.add(speedEncoder);

      final boolean idInTwoDirections = false;
      idEncoder = new SimpleIntEncodedValue(prefix + "id", 31, idInTwoDirections);
      registerNewEncodedValue.add(idEncoder);
  }

  @Override
  public IntsRef handleWayTags(final IntsRef edgeFlags, final ReaderWay way, final EncodingManager.Access accept,
      final long relationFlags) {
    if (accept.canSkip()) {
      return edgeFlags;
    }

    final Link link = castToLink(way);

    accessEnc.setBool(false, edgeFlags, link.getSpeedInKilometersPerHour() > 0.0);
    accessEnc.setBool(true, edgeFlags, link.getReverseSpeedInKilometersPerHour() > 0.0);

    setSpeed(false, edgeFlags, link.getSpeedInKilometersPerHour());
    setSpeed(true, edgeFlags, link.getReverseSpeedInKilometersPerHour());

    idEncoder.setInt(false, edgeFlags, Math.toIntExact(link.getId()));

    return edgeFlags;
  }

  public int getId(final IntsRef edgeFlags) {
    return idEncoder.getInt(false, edgeFlags);
  }

  @Override
  public double getMaxSpeed() {
      return maximumSpeed;
  }

  @Override
  public String toString() {
    return NAME;
  }
}
