## Context

The current update flow (abbreviated):

```java
// ObservableEntityList, parse thread
@OnEntityUpdated
protected void onUpdate(Entity entity, FieldPath[] fieldPaths, int num) {
    var i = entity.getIndex();
    var fieldPathsCopy = new FieldPath[num];
    System.arraycopy(fieldPaths, 0, fieldPathsCopy, 0, num);
    var state = entity.getState().copy();                       // FULL COPY
    pendingActions.add(() -> entities[i].performUpdate(
        ctx.getTick(), fieldPathsCopy, state));
}
```

The `state.copy()` happens on every updated entity every tick. The snapshot is passed to `ObservableEntity.performUpdate` and replaces the entity's `state` field; subsequent `ObservableEntityPropertyBinding.computeValue()` calls read against whatever state is currently stored.

Two reasons the copy cannot simply go away:

1. The FX action may run arbitrarily late; parse-side state has moved on.
2. A binding for a field that didn't change this tick may still be invalidated and re-read (e.g., cross-entity recalculation triggered by something else); the stored snapshot has to have that field available too.

`primitive-state-accessors` on the clarity side addresses (1) by providing a sparse `StateDelta`. Handling (2) requires keeping a long-lived analyzer-owned `State` that accumulates deltas across ticks — then the "stored snapshot" is always complete, just never freshly allocated.

## New flow

```java
// parse thread
@OnEntityUpdated
protected void onUpdate(Entity entity, FieldPath[] fieldPaths, int num) {
    var i = entity.getIndex();
    var fpsCopy = Arrays.copyOf(fieldPaths, num);
    var delta = entity.getState().captureChanged(fieldPaths, num);   // sparse, sized to num
    pendingActions.add(() -> entities[i].performUpdate(
        ctx.getTick(), fpsCopy, delta));
}

// FX thread
public void performUpdate(int tick, FieldPath[] changed, StateDelta delta) {
    for (var fp : changed) {
        fxState.applyFrom(delta, fp);                                // per-field primitive merge
    }
    invalidateBindingsFor(changed);
}
```

The entity's `fxState` is seeded on `@OnEntityCreated` via a full capture (still a one-time cost per entity lifetime), and mutated in place forever after.

## Decisions

### Single persistent state per entity vs. copy-on-write chain

Chosen: single persistent state, mutated in place on the FX thread. The FX thread is the sole owner after seeding; no concurrent reader needs an older view. A copy-on-write chain was considered to support "historical state at tick T" but analyzer doesn't currently have a tick-rewind feature driving that need; scope creep.

### Seed on create: new capture API or keep `state.copy()`

Chosen: keep using the full `state.copy()` for the create-time seed. Rationale: once-per-entity-lifetime is not a hot path. `State.copy()` is known to exist and work today — shipping analyzer calls it on every update and runs — so no new clarity-side API is needed for the seed path.

### Property count change: full or sparse?

Chosen: full capture. Count changes are rare (e.g., variable-array resize) and the event semantic is "state layout may have reshaped" — capturing selectively would require knowing which paths to capture after the layout change, which is exactly the information the count-change event doesn't give us. Full capture is safe and rare enough that allocation is a non-issue.

### FX-thread mutation safety

The analyzer's threading invariant today is: parse thread runs processors and schedules actions; FX thread drains and applies them in order. Adding an FX-thread-mutable `fxState` preserves this: parse thread never touches it. The only handoff is the immutable `StateDelta` (safely published via the action queue).

### Primitive-typed JavaFX binding accessors

Chosen: add `getIntPropertyBinding(String, int)`, `getLongPropertyBinding(String, long)`, `getFloatPropertyBinding(String, float)` alongside the existing `getPropertyBinding(Class<T>, String, T)`. Return types are JavaFX's primitive-specialized read-only properties (`ReadOnlyIntegerProperty`, etc.).

Call-site migration is voluntary: the 15+ existing `getPropertyBinding(Integer.class, …)` calls in `map/**` can switch opportunistically. No mass rewrite in this change.

### Keep `ObservableEntityProperty.valueProperty() : ObservableValue<Object>`?

Chosen: yes, unchanged. This is the spine of the property-table view and every cell renderer. Changing it affects too much surface for this change. Primitive paths are offered as an alternative, not a replacement.

## Risks and mitigations

- **Binding invalidation correctness.** Today, bindings invalidate when `performUpdate` replaces the snapshot. With in-place mutation, we must explicitly invalidate each binding whose `FieldPath` was in the `changed` array. Mitigation: an existing `invalidateBindingsFor(FieldPath[])` or equivalent is wired; verify during implementation, add if absent.
- **Delta lifetime.** The action queue holds the `StateDelta` until the FX thread consumes it. Deltas are small (sized to changed-field count) but allocated per-update-per-entity; no pooling proposed initially. If FX hitches appear under heavy tick-rate replays, consider a thread-local delta pool.

## Alternatives considered

- **Capture only fields with active bindings.** Elegant (zero waste) but requires analyzer to tell clarity which paths it cares about per entity, then clarity to intersect with the changed set. Extra coupling for a marginal gain over "capture everything that changed." Rejected.
- **Defer capture until FX thread asks.** Impossible — by then parse state has moved on. This is exactly why `copy()` exists today.
- **Pool `StateDelta` instances.** Potentially useful but adds lifecycle complexity. Defer until numbers show it matters.
