package nl.dat.routingmapmatcher.graphhopper;

import com.graphhopper.reader.ReaderRelation;
import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.AbstractFlagEncoder;
import com.graphhopper.routing.util.EncodedDoubleValue;
import com.graphhopper.routing.util.EncodedValue;

import nl.dat.routingmapmatcher.dataaccess.dto.NdwLinkDto;

public class NdwLinkFlagEncoder extends AbstractFlagEncoder {

  public static final String NAME = "ndw_links";

  private static final double SPEED = 10.0;

  private EncodedValue reversedEncoder;
  private EncodedValue indexEncoder;

  public NdwLinkFlagEncoder() {
    this(5, 1.0, 0);
  }

  public NdwLinkFlagEncoder(final int speedBits, final double speedFactor, final int maxTurnCosts) {
    super(speedBits, speedFactor, maxTurnCosts);
    maxPossibleSpeed = 31;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @Override
  public long handleRelationTags(final ReaderRelation relation, final long oldRelationFlags) {
    return oldRelationFlags;
  }

  @Override
  public long acceptWay(final ReaderWay way) {
    final NdwLinkDto ndwLink = castToNdwLink(way);
    final boolean access = ndwLink.isForwardAccess() || ndwLink.isBackwardAccess();
    return access ? 1 : 0;
  }

  private NdwLinkDto castToNdwLink(final ReaderWay way) {
    final NdwLinkDto ndwLink = (NdwLinkDto) way;
    if (ndwLink == null) {
      throw new IllegalStateException("Only NdwLinkDto are supported by this flag encoder");
    }
    return ndwLink;
  }

  @Override
  public int defineWayBits(final int index, final int originalShift) {
    int shift = super.defineWayBits(index, originalShift);

    speedEncoder = new EncodedDoubleValue("Speed", shift, speedBits, speedFactor, 10, maxPossibleSpeed);
    shift += speedEncoder.getBits();

    reversedEncoder = new EncodedValue("Reversed", shift, 1, 1.0, 0, 1);
    shift += reversedEncoder.getBits();

    indexEncoder = new EncodedValue("Index", shift, 31, 1.0, 0, Integer.MAX_VALUE);
    shift += indexEncoder.getBits();

    return shift;
  }

  @Override
  public long handleWayTags(final ReaderWay way, final long allowed, final long originalFlags) {
    final NdwLinkDto ndwLink = castToNdwLink(way);

    long flags = setAccess(originalFlags, ndwLink.isForwardAccess(), ndwLink.isBackwardAccess());
    flags = setSpeed(flags, SPEED);

    flags = reversedEncoder.setValue(flags, 0);

    flags = indexEncoder.setValue(flags, ndwLink.getIndex());

    return flags;
  }

  @Override
  public long reverseFlags(final long flags) {
    final long newFlags = reversedEncoder.setValue(flags, 1 - reversedEncoder.getValue(flags));
    return super.reverseFlags(newFlags);
  }

  public int getIndex(final long flags) {
    return (int) indexEncoder.getValue(flags);
  }

  public boolean isReversed(final long flags) {
    return reversedEncoder.getValue(flags) > 0;
  }

  @Override
  public String toString() {
    return NAME;
  }

}
