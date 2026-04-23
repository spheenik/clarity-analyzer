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

- [ ] 6.1 Add / extend `ObservableEntity` tests: persistent `fxState` survives multiple `performUpdate` calls; merged values are visible to bindings.
- [ ] 6.2 Add / extend `ObservableEntityList` tests: an update that changes three fields allocates a three-slot delta, not a full state copy.
- [ ] 6.3 Primitive binding smoke: a `getIntPropertyBinding` exposes the latest merged value after several updates.

> **Status**: Deferred. clarity-analyzer has no `src/test/` tree and no
> JUnit/TestFX dependency today; wiring a test harness is out of scope
> for this change. See design.md risks — binding-invalidation semantics
> are preserved by field-access (`ObservableEntityProperty`'s value
> supplier reads `ObservableEntity.this.fxState` at invocation time, so
> in-place mutation is visible without rewiring). Verified manually
> via `./gradlew build`.

## 7. Compile-and-start verification (no-GUI-launch rule)

- [x] 7.1 `./gradlew build` passes against the sibling clarity checkout.
- [ ] 7.2 `./gradlew packageUnoJar` produces a fat jar.
  - Pre-existing packaging bug: the uno-jar plugin rejects the three
    platform-specific `javafx-graphics-21.0.7-{win,linux,mac}.jar`
    runtimeOnly deps as duplicate entries. Reproduces on `HEAD~`
    without this change; unrelated. Tracked separately.
- [x] 7.3 Do NOT launch the GUI without user agreement (per `feedback_dtinspector_analyzer_compile_only`). Compile-only verification is the default.

## 8. Documentation

- [x] 8.1 Update `CLAUDE.md` in clarity-analyzer to note the FX-side persistent-state model and the new primitive binding accessors.
- [x] 8.2 Brief Javadoc on the new methods describing thread ownership (FX-thread only for `fxState` reads/writes).

## 9. Follow-ups (tracked, not in scope)

- [ ] 9.1 Benchmark FX-thread allocation rate during heavy scrubbing before/after; only if user reports hitches.
- [ ] 9.2 `StateDelta` pooling — consider only if per-update delta allocation shows up in profiling.
- [ ] 9.3 `ObservableEntityProperty` internal primitive specialization — larger refactor, explicitly out of scope here.
