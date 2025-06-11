# Change Log

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

Here we write upgrade notes. It's a team effort to make them as straightforward as possible.

## [18.0.0] - 2025-06-11

### Added

- Introduced `BaseMapMatcher`, a base class for map-matching functionality, which provides reusable mechanisms for handling profiles,
  custom models, and network access.
- Added new `createMapMatcher` method in `MapMatcherFactory` interface to support the use of `CustomModel` for advanced route
  configurations.

### Changed

- Refactored `SinglePointMapMatcher`, `StartToEndMapMatcher`,`ViterbiLineStringMapMatcher`,`Router`, to extend `BaseMapMatcher` for
  improved reuse and consistency, removing previously duplicated code related to profiles and custom models.

### Removed

- Removed `profileName` and `customModel` from `RoutingRequest` as they are handled by the MapMatcherFactory for consistency.

### Fixed

- Updated version numbers in `pom.xml` files to `18.0.0-SNAPSHOT` for consistency with the new release version.

## [17.0.0] - 2025-05-13

### Fixed
- Fixed an issue in RouteResponse where matched links were incorrectly merged at waypoints.
- Fixed RoutingLegResponseSequence.isPreviousRoutingLegEndOnNode(), which always returned true.

### Removed
- Breaking: removed method RouteResponse.getMatchedLinks() deemed confusing. Clients should either explicitly use route legs if they support
  this concept, or call RouteResponse.getMatchedLinksGroupedBySameLinkAndDirection() if they want a flat structure.

## [16.0.0] - 2025-03-04

### Changed

Update GraphHopper compatibility to version 10.2

Refactored various components and tests to align with GraphHopper 10.2 API changes, including method and class updates. Improved code
structure for QueryGraphWeightingAdapter.

## [15.0.0] - 2025-02-24

### Changed

- Replace reliability algorithm in `LineStringScoreUtil`: use only Fréchet distance instead of combination of Hausdorff distance and
  absolute length difference.

## [14.0.0] - 2024-11-20

### Added

- Introduced a new `absoluteRelativeWeighingFactor` configuration parameter in the `ViterbiLineStringMapMatcher` and `StartToEndMapMatcher`
  classes to enhance flexibility in scoring calculations.
- Added a `combinedWeighedDifference` method to calculate a weighed difference using both absolute and relative measurement differences.
- Enhanced logging to include the configuration of the `absoluteRelativeWeighingFactor` for debugging purposes.

### Changed

- Updated `LineStringScoreUtil` to utilize the new `absoluteRelativeWeighingFactor` for improved scoring of line string matches.

## [13.2.0] - 2024-11-19

### Fixed

- MINOR Handle error cases
- MINOR Removed obsolete fields added in 13.1.0

## [13.1.0] - 2024-11-18

### Added

- Added snap to nodes functionality for routing operation

## [13.0.3] - 2024-10-15

### Fixed

- MINOR Removed vulnerable transitive dependency com.google.protobuf:protobuf-java:3.12.2

## [13.0.0] - 2024-10-07

### Added

- properties edgeid and edge key to isochrone match dto

### Changed

### Fixed

- VirtualEdgeIteratorStateReverseExtractor throws class cast exception when VirtualEdgeIteratorState has an IteratorStateImpl member
- QueryGraphWeightingAdapter was blocking non start virtual edges in reverse direction.

## [12.0.0] - 2024-09-24

### Added

### Changed

### Fixed

- Correctly consider driving direction in isochrone when start point is on reversed link. Upstream links are now flipped
  on the library side instead of nls-routing-api, making this change breaking.

## [11.1.0] - 2024-09-13

### Added

- Add reverse link ID to GraphHopper network for FCD segments

### Changed

### Fixed

## [11.0.1] - 2024-09-06

### Added

- MINOR Added this changelog file

### Changed

### Fixed
