## Why

`ObservableEntityList` currently does `entity.getState().copy()` on every `@OnEntityUpdated`, `@OnEntityCreated`, and `@OnEntityPropertyCountChanged` callback so that an FX-thread action scheduled later can read arbitrary fields from a stable snapshot. Full-state allocation runs at the tick rate, for every mutated entity — allocation cost scales with entity size (all fields copied), not with the actually-changed field count (usually a handful).

The copy exists because the parse thread mutates a single in-place `State` object; by the time the FX-thread action runs the state has moved on. FX also needs access to fields that didn't change this tick (a binding can be invalidated and re-read against the latest known state), which forces today's everything-or-nothing snapshot.

Clarity's companion change `primitive-state-accessors` introduces `State.captureChanged(FieldPath[], num) → StateDelta`: a sparse per-field primitive snapshot sized to the changed-field count. With that, the natural shape is an analyzer-owned long-lived `State` per entity that absorbs sparse deltas over its lifetime — full copy is never needed after the initial seed.

## What Changes

- Add a long-lived FX-thread-owned `State` field on `ObservableEntity`, initialized once at creation time from a seed capture of the parse-side state.
- Replace the `state.copy()` call in `ObservableEntityList.onUpdate` with a `captureChanged(fieldPaths, num)` call that produces a `StateDelta`. The scheduled FX action receives the delta plus the changed `FieldPath[]` and merges each field into the entity's persistent `fxState` via `State.applyFrom`.
- Keep `@OnEntityCreated` using a full capture (via a new `captureAll` helper or the existing copy path) — this is once per entity lifetime, not per tick.
- `@OnEntityPropertyCountChanged` takes a full capture too (rare event, count-mutation reshapes the state layout).
- `ObservableEntityPropertyBinding.computeValue()` reads off `fxState` instead of the per-update copy — same logical view, no per-update allocation.
- Add primitive-typed JavaFX binding accessors where the type is statically known: `getIntPropertyBinding(String name, int defaultValue) → ReadOnlyIntegerProperty`, same for `Long`/`Float`. Existing `getPropertyBinding(Class<T>, String, T)` remains as the generic fallback.

## Capabilities

### New Capabilities
- `sparse-state-delta-updates`: FX-side per-entity persistent state that absorbs sparse per-tick deltas, replacing the current full-state-copy-per-update pattern. Creation and property-count-change events still take a full snapshot (rare events, lifecycle-scoped).
- `primitive-entity-bindings`: JavaFX primitive-specialized `ReadOnlyIntegerProperty` / `ReadOnlyLongProperty` / `ReadOnlyFloatProperty` bindings produced off the clarity primitive accessors, avoiding box-cast-unbox on the read path.

## Impact

- `src/main/java/skadistats/clarity/analyzer/replay/ObservableEntity.java` — adds `fxState` field, adds primitive binding accessors, changes `ObservableEntityPropertyBinding.computeValue()` read source.
- `src/main/java/skadistats/clarity/analyzer/replay/ObservableEntityList.java` — changes `onUpdate` to capture a delta instead of a full copy; `onCreate` and `onPropertyCountChange` keep a full capture (once-per-lifetime / rare-event).
- `src/main/java/skadistats/clarity/analyzer/map/**/*.java`, `src/main/java/skadistats/clarity/analyzer/map/icon/*.java`, `src/main/java/skadistats/clarity/analyzer/map/binding/*.java` — call sites of `getPropertyBinding(Integer.class, ...)` / `getPropertyBinding(Float.class, ...)` / `getPropertyBinding(Long.class, ...)` may migrate to the primitive-typed variants for no-box reads. Vector and generic Object paths unchanged.
- No FXML changes.
- No Lombok-generated API changes.

## Dependencies

- **Hard dependency**: clarity's `primitive-state-accessors` change must land first. This change cannot compile without `State.captureChanged`, `State.applyFrom`, `StateDelta`, and the primitive getters on `State` / `Entity`.
- Composite-build wiring (clarity sibling checkout) means local integration testing is straightforward once the clarity side is on the same branch.

## Non-goals

- **`ObservableEntityProperty` primitive specialization.** The per-property wrapper class currently exposes `valueProperty() : ObservableValue<Object>`. Switching it to primitive-specialized JavaFX observables internally is a larger refactor affecting every cell renderer and property view; scoped out. The proposed `getIntPropertyBinding` / etc. sit alongside, producing primitive JavaFX properties without reshaping `ObservableEntityProperty` itself.
- **Push-side primitive dispatch.** The clarity change explicitly defers primitive `@OnEntityPropertyChanged` handlers. Analyzer doesn't use that annotation anyway; non-issue for this change.
- **Parse-thread allocation.** This change is strictly about FX-side allocation during interactive scrubbing. Parse-thread cost is unchanged.
