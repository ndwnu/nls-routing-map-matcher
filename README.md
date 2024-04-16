# NLS routing map matcher library

This library is a fork of the [ndw-ndss-dat-mapmatching](https://dev.azure.com/ndwnu/NLS/_git/ndw-ndss-dat-mapmatching)
project. From this project the code from the [graphhopper](https://www.graphhopper.com/) based routing-mapmatcher is
used as a basis for this library.

The routing map matcher uses route search algorithms to map linestring geometries on a target map's network. This allows
for direct mapping between different maps without the need for an intermediate 'basemap'. In order to do this the client
applications need to create linestring geometries for their sourcemap and use those to match them with the target
network.

## GitHub vs Azure DevOps
This project is maintained by Nationaal Dataportaal Wegverkeer (NDW)
* It is primarily maintained within Azure Devops and mirrored to https://github.com/ndwnu/nls-routing-map-matcher 
* It works within NDW infrastructure, with some of its constraints;
* This repository contains functionality only, data is gathered from APIs/database, but not included;
* Pipelines are not designed to be used within GitHub;
* GitHub wiki and issues aren't enabled.

## Design


| Library Class                     | Graphhopper Class           | Description                                                                                                                                                                                                   |
|-----------------------------------|-----------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| NetworkGraphHopper                | GraphHopper                 | Initializes for a specific network, core class to perform routing based actions on                                                                                                                            |
| NetworkReader                     | -                           | Reads our Links into a Graphhopper BaseGraph                                                                                                                                                                  |
| Link                              | ReaderWay                   | Or abstract Link represents a way (road section) received from a network. Extend this and add custom data                                                                                                     |
| CustomVehicleEncodedValues        | VehicleEncodedValues        | Defines HOW to encode access/average speed for our vehicle (which encoded key, bits, etc) (we do not implement: priority, turn restrictions). We use a hard coded default configuration for all our vehicles. |                                                                      
| CustomVehicleEncodedValuesFactory | VehicleEncodedValuesFactory | Creates 0..n vehicles, returns our CustomVehicleEncodedValues ↑                                                                                                                                               |
| LinkVehicleParserFactory          | VehicleTagParserFactory     | Creates our VehicleTagParsers instance. Deducts WHAT average speed to encode for this vehicle based on road section data. Uses LinkAccessParser and LinkAverageSpeedParser                                    |
| LinkAccessParser                  | TagParser                   | Encodes a boolean that defines if it is accessible for this vehicle. Evaluates a link using the LinkVehicleMapper#getAccessibility(link)                                                                      |
| LinkAverageSpeedParser            | AbstractAverageSpeedParser  | Encodes a decimal that defines the average speed for this vehicle. Evaluates a link using the LinkVehicleMapper#getSpeed(link)                                                                                |
| LinkVehicleMapper                 | -                           | Mapper to determine accessibility and average speed for a specific Link type. Mappers are resolved by looking for spring context beans. Used by LinkAccessParser and LinkAverageSpeedParser                   |
| -                                 | VehicleTagParsers           | Contains tag parsers for access, speed and priority                                                                                                                                                           |  
| LinkVehicleTagParsersFactory      | VehicleTagParserFactory     | Creates 0..n vehicle tag parsers, returns VehicleTagParsers with our CustomAccessParser ↑ and CustomAverageSpeedParser                                                                                        |
| -                                 | EncodedValue                | Defines how a value is encoded in a network (key, bits, directional or not)                                                                                                                                   |
| EncodedMapperFactoryRegistry      | -                           | Registry for looking up EncodedMapperFactory by java type. Wires all EncodedMapperFactory instances found in the spring boot context                                                                          |
| EncodedValueFactoryRegistry       | -                           | Registry for looking up AbstractEncodedValueFactory by java type. Wires all AbstractEncodedValueFactory instances found in the spring boot context                                                            | 
| AnnotatedEncodedMapperFactory     | TagParserFactory            | Creates AbstractEncodedMapper (TagParser) instances for @EncodedValue annotated link properties                                                                                                               |
| AnnotatedEncodedValueFactory      | EncodedValueFactory         | Creates an EncodedValue instances for @EncodedValue annotated link properties                                                                                                                                 |
| AbstractEncodedMapper             | TagParser                   | Is able to retrieve a value from a field annotated with @EncodedValue and knows which encoder to use to encode the value into the graphhopper network                                                         |
| EncodedStringValueFactory         | -                           | Creates EncodedValue instances of StringEncodedValue                                                                                                                                                          |
| EncodedIntegerValueFactory        | -                           | Creates EncodedValue instances of IntEncodedValueImpl                                                                                                                                                         |
| EncodedLongValueFactory           | -                           | Creates EncodedValue instances of IntEncodedValueImpl, so it's basically the same as EncodedIntegerValueFactory, it is required for encoding our Link.id, which is of type long                               |
| EncodedDoubleValueFactory         | -                           | Creates EncodedValue instances of DecimalEncodedValueImpl                                                                                                                                                     |
| EncodedBooleanValueFactory        | -                           | Creates EncodedValue instances of SimpleBooleanEncodedValue                                                                                                                                                   |
| -                                 | Profile                     | Has a name, a vehicle name and a CustomModel. Vehicle name is used key for CustomVehicleEncodedValuesFactory                                                                                                  |
| -                                 | CustomModel                 | Allows customizing a profile with distance influences and expressions to restrict road access                                                                                                                 |
| @EncodedValue                     | -                           | Annotation used on Link classes to add meta data about how to encode fields into a network. Is used for configuring TagParsers en EncodedValues.                                                              |
| DirectionalDto<T>                 | -                           | Field that can be used in a custom Link to describe directional road information                                                                                                                              |

`NetworkGraphHopper` is created with a link supplier to return `Link` extending objects for creating the network. NetworkReader
iterates over the links and adds them in a baseGraph. It stores:

Link data:
 - id (way_id)
 - from, 
 - to, 
 - distanceInMeters
 - geometry, 

Link extended data:
 - Link extending class fields, annotated with @EncodedValue

For each vehicle configured in profiles:
   - Consults LinkVehicleMapper to acquire:
     - accessibility (boolean)
     - average speed (decimal)

## Project setup

This library is build for spring boot.

### Modules

It only has one module

## Getting started

Include dependency in maven: 
```pom
    <dependency>
        <groupId>nu.ndw.nls</groupId>
        <artifactId>routing-map-matcher-library</artifactId>
        <version>8.0.0</version>
    </dependency>
```

Import the configuration:
```java
@Import({RoutingMapMatcherConfiguration.class})
```

Define your domain specific link, example:

```java
public class TestLink extends Link {
    @Builder(builderMethodName = "testLinkBuilder")
    protected NwbLink(long id, long fromNodeId, long toNodeId, double distanceInMeters, LineString geometry,
            double speedInKilometersPerHour, double reverseSpeedInKilometersPerHour) {
        super(id, fromNodeId, toNodeId, speedInKilometersPerHour, reverseSpeedInKilometersPerHour, distanceInMeters,
                geometry);
    }
}
```

Add custom domain specific link fields, annotate them with `@EncodedValue` and make sure they have proper getter 
methods:
```java

    @EncodedValue(key = "bothDirectionValue", bits=7)
    private final Integer bothDirectionValue;

    @EncodedValue(key = "truck_access_forbidden")
    private final DirectionalDto<Boolean> truckAccessForbidden;
    
```
You can use values types:
- Integer (int)
- Double (double)
- Boolean (boolean)
- String

When you need directional data, you must wrap your value inside a `DirectionalDto`, which has a forward and reverse 
property, and use the generic type argument to specify which (boxed) type you want to use.

You can also annotate a field with a custom java type, but then you also need to provide implementations of the 
following classes and make them available in the spring boot context:
- EncodedMapperFactory
- AbstractEncodedMapper
- AbstractEncodedValueFactory 

For each custom link and vehicle name used in profiles, add a mapper and make it available in the spring boot context:
- `LinkVehicleMapper`

This mapper maps the average speed and accessibility of your vehicle and uses your custom link implementation to 
determine the values.

Configure settings and supply a link supplier for your custom link type:
```java
RoutingNetworkSettings<TestLink> routingNetworkSettings = RoutingNetworkSettings.<TestLink>builder()
        .networkNameAndVersion("test_network")
        .linkSupplier(links::iterator)
        .build();
```

Autowire `GraphHopperNetworkService` and call one of these methods with the configuration to initialize a 
`NetworkGraphHopper` instance:
- inMemory
- loadFromDisk

Or call the following method to just persist the network to disk:
- storeOnDisk

The methods above requires the following arguments:
 - linkClass: Your specific Link class type
 - `List<Profile>`: One or more profiles, which reference a vehicle name
 - `RoutingNetworkSettings`: as configured
 - graphhopperRootPath: Root path where all graphhopper data is persisted

You now have an initialized `NetworkGraphHopper` that you can use
 
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
RoutingNetworkSettings routingNetworkSettings = RoutingNetworkSettings.builder()
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
