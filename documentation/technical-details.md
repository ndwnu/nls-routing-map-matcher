## Bidirectional routing and edge orientation in combination with search direction

Our road accessibility is **not encoded in the GraphHopper graph** using an `EncodedValue`. Instead, accessibility is determined at runtime
by looking up the corresponding NWB road section in a cache.

The NWB stores accessibility relative to the **original road section direction**:

* `forwardAccessible` — travel is allowed in the NWB direction.
* `backwardAccessible` — travel is allowed opposite to the NWB direction.

GraphHopper, however, does not always present an edge in its original orientation.

### Edge orientation

An `EdgeIteratorState` can represent the same physical edge in two orientations.

Original NWB road section:

```text
A ----------> B
```

Normal orientation:

```text
base = A
adj  = B
edgeState.reverse = false
```

Reversed orientation:

```text
base = B
adj  = A
edgeState.reverse = true
```

The `edgeState.reverse` flag indicates whether the current `EdgeIteratorState` is reversed relative to the stored edge.

### Search direction

The `reverse` parameter passed to `Weighting.calcEdgeWeight(...)` has a different meaning.

* `reverse == false` indicates the **forward search** (from source).
* `reverse == true` indicates the **backward search** (from target) used by bidirectional routing.

It does **not** directly indicate whether the vehicle is travelling opposite to the original road section direction.

### Determining the actual travel direction

To determine whether traversal is forward or backward relative to the original NWB road section, both the edge orientation and the search
direction must be taken into account.

This is an exclusive OR (XOR) operation.

| Edge orientation (`edgeState.reverse`) | Search (`reverse`) | Traversal backward relative to NWB |
|----------------------------------------|--------------------|------------------------------------|
| false                                  | false              | false (forward)                    |
| false                                  | true               | true (backward)                    |
| true                                   | false              | true (backward)                    |
| true                                   | true               | false (forward)                    |

This corresponds to an exclusive OR (XOR):

```java
boolean traversedInReversedDirection = reverseSearch ^ edgeIsReversed;
```

where:

* `edgeIsReversed` indicates whether the `EdgeIteratorState` is reversed relative to the original NWB road section.
* `reverseSearch` indicates whether GraphHopper is executing the backward search.

### Why this is necessary

This issue became particularly clear on **one-way roads**.

A standard (forward-only) Dijkstra search only evaluates edges in the forward search direction, so the accessibility check is always
performed against the expected road orientation.

Bidirectional Dijkstra, however, performs a simultaneous search from both the source and the destination. During the backward search,
GraphHopper may present an edge in the opposite orientation while also indicating that it is part of the reverse search. If accessibility is
determined using only the `reverse` parameter, the road can be evaluated against the wrong NWB direction.

For one-way roads, this means the backward search may incorrectly conclude that a perfectly valid edge is not traversable, preventing the
forward and backward search frontiers from connecting. As a result, `Dijkstra` can successfully find a route while `DijkstraBidirection`
reports that no route exists.

By combining the edge orientation (`edgeState.reverse`) with the search direction (`reverse`) using an XOR, accessibility is always
evaluated relative to the original NWB road section direction. This ensures that both forward and bidirectional routing interpret one-way
roads consistently.
