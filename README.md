# NLS routing map matcher library

This library is a fork of the [ndw-ndss-dat-mapmatching](https://dev.azure.com/ndwnu/NLS/_git/ndw-ndss-dat-mapmatching)
project. From this project the code from the [graphhopper](https://www.graphhopper.com/) based routing-mapmatcher is
used as a basis for this library.

The routing map matcher uses route search algorithms to map linestring geometries on a target map's network. This allows
for direct mapping between different maps without the need for an intermediate 'basemap'. In order to do this the client
applications need to create linestring geometries for their sourcemap and use those to match them with the target
network.

## Project setup

The project is a multi-module maven project.

### Modules

- map-matcher-library:
  The core library


- map-matcher-spring-boot-autoconfigure:
  The spring based autoconfiguration of the library for use within a spring project.

## General usage

The routing map matcher requires the client application to create a RoutingNetwork entity for the target network as well
as a MapMatchingRequest entity for the source linestrings to match with the target network. Both these entities can be
used to call the RoutingMapMatcher's
"*matchLocations*" method. This method will return a java stream of LineStringMatch entities. See source code for more
information on the entity's attributes.

### RoutingNetwork

The RoutingNetwork allows routing map matcher library to create a target routing network. The RoutingNetwork has the
following attributes:

```java
public class RoutingNetwork {
  String networkNameAndVersion;
  Supplier<Iterator<Link>> linkSupplier;
}
```

* networkNameAndVersion:

  A client application's unique name version combination for this network.

  Provide alphanumeric characters with no spaces ( '_' or '-' are allowed to indicate spaces)


* linkSupplier:

  A supplier lambda function providing the network's link entities.

Example usage:

```java 
RoutingNetwork routingNetwork = RoutingNetwork.builder()
        .networkNameAndVersion("FCD_" + fcdVersion)
        .linkSupplier(() -> linksIterator)
        .build();
```

### MapMatchingRequest

The MapMatchingRequest provides the routing map matcher library with the source linestring geometries to do the matching
on the target network.

The MapMatchingRequest has the following attributes:

```java
public class MapMatchingRequest {
  String locationTypeName;
  Supplier<List<LineStringLocation>> locationSupplier;
}
```

* locationTypeName:

  A client application's unique name for the source linestring matching.

  Only used for logging purposes.


* locationSupplier:

  A supplier lambda function providing the map matching linestring geometries.

Example usage:

```java
MapMatchingRequest mapMatchingRequest = MapMatchingRequest.builder()
        .locationSupplier(() -> basemapRepository.getTrajectories(handle, nwbVersion, fcdVersion))
        .locationTypeName("NWB trajectories")
        .build();
```

## Configuration

### spring boot

When using spring boot in your project, you can use the spring boot dependency to use the routing map matcher
library:

```xml
<dependency>
  <groupId>nu.ndw.nls</groupId>
  <artifactId>routing-map-matcher-spring-boot</artifactId>
  <version>${routing-map-matcher.version}</version>
</dependency>
```

```java
@Import(MapMatcherConfiguration.java)
```



This will give you a pre-configured bean called "*RoutingMapMatcher*" to use in your application.

### Plain Java

If your application does not use spring boot you can use the maven routing-map-matcher-library dependency:

```xml
<dependency>
  <groupId>nu.ndw.nls</groupId>
  <artifactId>routing-map-matcher-library</artifactId>
  <version>${routing-map-matcher.version}</version>
</dependency>
```

In order to use the routing map matcher library in your application the following configuration needs to be done.
RoutingMapMatcher can be used as a singleton in your application.

```java
public RoutingMapMatcher routingMapMatcher() {
        return new RoutingMapMatcher(new ViterbiLinestringMapMatcherFactory(
                new NetworkGraphHopperFactory());
}
```
