## Bidirectional routing and edge orientation in combination with search direction

### Edge orientation

An `EdgeIteratorState` can represent the same physical edge in two orientations.

Original road section:

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

To determine whether traversal is forward or backward relative to the original road section, both the edge orientation and the search
direction must be taken into account.

This is an exclusive OR (XOR) operation.

| Edge orientation (`edgeState.reverse`) | Search (`reverse`) | Traversal backward relative to |
|----------------------------------------|--------------------|--------------------------------|
| false                                  | false              | false (forward)                |
| false                                  | true               | true (backward)                |
| true                                   | false              | true (backward)                |
| true                                   | true               | false (forward)                |

This corresponds to an exclusive OR (XOR):

```java
boolean traversedInReversedDirection = reverseSearch ^ edgeIsReversed;
```

where:

* `reverseSearch` indicates whether GraphHopper is executing the backward search.
* `edgeIsReversed` indicates whether the `EdgeIteratorState` is reversed relative to the original road section.
