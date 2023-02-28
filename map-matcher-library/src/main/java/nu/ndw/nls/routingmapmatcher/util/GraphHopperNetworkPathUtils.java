package nu.ndw.nls.routingmapmatcher.util;

import java.nio.file.Path;

public final class GraphHopperNetworkPathUtils {

    private GraphHopperNetworkPathUtils() {
    }

    public static String formatNetworkNameAndVersion(String networkNamePrefix, String version) {
        return networkNamePrefix + version;
    }

    public static Path formatNormalizedPath(Path rootDirectory, String networkNamePrefixAndVersion) {
        return rootDirectory.resolve(networkNamePrefixAndVersion).normalize();
    }

    public static Path formatNormalizedPath(Path rootDirectory, String networkNamePrefix, String version) {
        return formatNormalizedPath(rootDirectory, formatNetworkNameAndVersion(networkNamePrefix, version));
    }
}
