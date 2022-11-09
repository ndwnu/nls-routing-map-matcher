package nu.ndw.nls.routingmapmatcher.util;

import java.nio.file.Path;

public final class GraphHopperNetworkPathUtils {

    private GraphHopperNetworkPathUtils() {
    }

    public static String formatNetworkNameAndVersion(final String networkNamePrefix, final String version) {
        return networkNamePrefix + version;
    }

    public static Path formatNormalizedPath(final Path rootDirectory, final String networkNamePrefixAndVersion) {
        return rootDirectory.resolve(networkNamePrefixAndVersion).normalize();
    }

    public static Path formatNormalizedPath(final Path rootDirectory, final String networkNamePrefix,
            final String version) {
        return formatNormalizedPath(rootDirectory, formatNetworkNameAndVersion(networkNamePrefix, version));
    }
}
