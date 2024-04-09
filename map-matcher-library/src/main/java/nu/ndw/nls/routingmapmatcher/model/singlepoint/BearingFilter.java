package nu.ndw.nls.routingmapmatcher.model.singlepoint;

public record BearingFilter(int target, int cutoffMargin) {

    public static nu.ndw.nls.geometry.bearing.model.BearingFilter toGeometryFilter(BearingFilter bearingFilter) {
        if (bearingFilter == null) {
            return null;
        }
        return new nu.ndw.nls.geometry.bearing.model.BearingFilter(bearingFilter.target(),
                bearingFilter.cutoffMargin());
    }
}
