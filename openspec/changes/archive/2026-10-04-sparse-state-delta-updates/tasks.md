## 0. Prerequisite

- [x] 0.1 Confirm clarity's `primitive-state-accessors` change is merged (or available on the sibling checkout). Without `State.captureChanged`, `State.applyFrom`, `StateDelta`, and primitive getters on `State` / `Entity`, this change does not compile.

## 1. ObservableEntity persistent FX state

- [x] 1.1 Add a `fxState : State` field to `ObservableEntity`. Initialized lazily on first `performCreate`.
- [x] 1.2 Change `performCreate(tick)` to accept a full `State` (seed snapshot from the parse side) and assign it to `fxState`.
- [x] 1.3 Change `performUpdate(tick, FieldPath[], StateDelta)` signature: replace the old `State` snapshot parameter with the delta.
- [x] 1.4 Implement delta-merge loop inside `performUpdate`: for each changed `FieldPath`, call `fxState.applyFrom(delta, fp)`.
- [x] 1.5 Invalidate `ObservableEntityPropertyBinding` instances whose `FieldPath` appears in the changed array. Verify the existing invalidation path handles this; extend if needed.

## 2. ObservableEntityList update dispatch

- [x] 2.1 In `onCreate`: keep full-state seed capture using `entity.getState().copy()` (unchanged from current shipping code).
- [x] 2.2 In `onUpdate`: replace `entity.getState().copy()` with `entity.getState().captureChanged(fieldPaths, num)`. Pass the resulting `StateDelta` to the scheduled action.
- [x] 2.3 In `onPropertyCountChange`: keep full-state snapshot (rare event, layout-reshape semantic).
- [x] 2.4 Update all three corresponding `pendingActions.add(...)` closures to the new performUpdate / performCreate / performCountChanged signatures.

## 3. ObservableEntityPropertyBinding read source

- [x] 3.1 `computeValue()` reads from the owning `ObservableEntity.fxState` rather than a per-update snapshot.
- [x] 3.2 Verify the cross-entity composition case (`DOTAS1PositionBinder`, `DeferringPositionBinder`, `CSGOS2AndDeadlockPositionBinder`) still works: bindings spanning multiple entities should each read off their own `fxState`.

## 4. Primitive-typed JavaFX binding accessors

- [x] 4.1 Add `getIntPropertyBinding(String name, int defaultValue) : ReadOnlyIntegerProperty` on `ObservableEntity`. Resolves the `FieldPath` via `getFieldPathForName`; binding `computeValue` reads `fxState.getInt(fp)`; invalidated on the same path as the generic binding.
- [x] 4.2 Add `getLongPropertyBinding(String, long)` and `getFloatPropertyBinding(String, float)` following the same pattern.
- [x] 4.3 Return a default-valued wrapper (`SimpleIntegerProperty(defaultValue)` equivalent as a read-only view) when the field path cannot be resolved, matching the existing generic path's default behavior.

## 5. Opportunistic call-site migration

- [x] 5.1 Audit call sites of `oe.getPropertyBinding(Integer.class, ...)`, `Long.class`, `Float.class` in `map/binding/`, `map/icon/`, `map/position/`.
- [x] 5.2 Migrate the obvious ones to `getIntPropertyBinding` / `getLongPropertyBinding` / `getFloatPropertyBinding`. Skip where the downstream `.map(...)` chain expects a boxed type.
  - Audit result: every primitive call site either (a) feeds into an `EasyBind.combine/.map` chain whose lambda expects boxed `Integer`/`Long`/`Float` (`DOTAS1/S2PositionBinder`, `CSGOS1/S2AndDeadlockPositionBinder`, `DeferringPositionBinder`, `DotaS2BindingGenerator.bindPlayerResource`), or (b) is wrapped in `selectInteger(...)` inside an `IntegerBinding`-typed accessor whose return type is consumed as `IntegerBinding` by other intermediate helpers (`EntityIcon.getPlayerId / getTeamNum / getModelHandle`). No clean drop-in migration under the task 5.2 skip rule.
- [x] 5.3 Leave `Vector.class` and any other non-primitive call sites on the generic API.

## 6. Tests

- [x] 6.1 Add / extend `ObservableEntity` tests: persistent `fxState` survives multiple `performUpdate` calls; merged values are visible to bindings.
- [x] 6.2 Add / extend `ObservableEntityList` tests: an update that changes three fields allocates a three-slot delta, not a full state copy.
- [x] 6.3 Primitive binding smoke: a `getIntPropertyBinding` exposes the latest merged value after several updates.

> All three are covered by `SparseStateDeltaUpdatesTest` (TestNG, added with
> this change). It replays `dota/s2/normal/1648457986.dem` through the same
> create / `captureChanged` + `performUpdate` / `performCountChanged` sequence
> `ObservableEntityList` produces, synchronously and without the FX toolkit,
> and periodically compares every mirrored entity with the live one. 6.2 is
> asserted at the delta level (`delta.fields().length == num` for every
> update) rather than through `ObservableEntityList`, whose handlers post to
> the FX-side pending-action queue. Disabling the `applyFrom` call in
> `performUpdate` makes the test fail. The test skips when the replay is not
> available (`-Pclarity.replays=<dir>`).

## 7. Compile-and-start verification (no-GUI-launch rule)

- [x] 7.1 `./gradlew build` passes against the sibling clarity checkout.
- [x] 7.2 `./gradlew packageUnoJar` produces a fat jar.
  - Fixed separately alongside this change: the uno-jar plugin
    (`com.needhamsoftware.unojar:gradle-plugin:1.1.0`) iterates
    `runtimeClasspath`'s resolved artifacts without deduping by file or
    `(coord, classifier)`. Two independent triggers removed: the
    host-platform `javafx-graphics` manual classifier (dropped; the
    `org.openjfx.javafxplugin` already adds it) and the ancient
    `javafx-base:14` pulled transitively by `easybind` (excluded; a
    current `javafx-base` still arrives via `javafx-graphics`). Plugin
    bug itself remains — filed as a separate workstream.
- [x] 7.3 Do NOT launch the GUI without user agreement (per `feedback_dtinspector_analyzer_compile_only`). Compile-only verification is the default.

## 8. Documentation

- [x] 8.1 Update `CLAUDE.md` in clarity-analyzer to note the FX-side persistent-state model and the new primitive binding accessors.
- [x] 8.2 Brief Javadoc on the new methods describing thread ownership (FX-thread only for `fxState` reads/writes).

## 9. Follow-ups (tracked, not in scope)

- [x] 9.1 Benchmark FX-thread allocation rate during heavy scrubbing before/after; only if user reports hitches. *(deferred — not in scope, follow-up only if needed)*
- [x] 9.2 `StateDelta` pooling — consider only if per-update delta allocation shows up in profiling. *(deferred — not in scope, follow-up only if needed)*
- [x] 9.3 `ObservableEntityProperty` internal primitive specialization — larger refactor, explicitly out of scope here. *(deferred — not in scope, follow-up only if needed)*
