# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

Here we write upgrade notes. It's a team effort to make them as straightforward as possible.

## [13.2.0] - 2024-11-19

### Fixed
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
