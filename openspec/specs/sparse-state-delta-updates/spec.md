# sparse-state-delta-updates Specification

## Purpose

The analyzer's FX side keeps one persistent `EntityState` per entity, seeded from a full copy at creation and updated in place from sparse clarity `StateDelta`s captured on the parse thread, instead of copying the whole entity state on every update.

## Requirements
### Requirement: Persistent FX-side entity state

Each `ObservableEntity` SHALL hold a long-lived `State` instance (`fxState`) owned by the FX thread. `fxState` SHALL be seeded once at entity creation from a full snapshot of the parse-side state, then mutated in place on every subsequent update by merging a `StateDelta` into it. Parse thread SHALL NOT read or mutate `fxState`.

#### Scenario: Seed at creation

- **WHEN** `@OnEntityCreated` fires for a new entity
- **THEN** the parse thread captures a full snapshot of the entity's state and schedules an FX-thread action that assigns it as the new `ObservableEntity`'s `fxState`

#### Scenario: Per-tick merge preserves unchanged fields

- **WHEN** a tick arrives with `@OnEntityUpdated` reporting that fields `fpA` and `fpB` changed, but not `fpC`
- **AND** the FX action runs `fxState.applyFrom(delta, fpA)` and `fxState.applyFrom(delta, fpB)`
- **THEN** subsequent reads of `fxState.getInt(fpC)` SHALL return the value from before this tick, unchanged; reads of `fpA` and `fpB` SHALL return the new values

#### Scenario: Parse thread does not touch fxState

- **WHEN** a consumer inspects the concurrency model
- **THEN** the only code paths that read or write `fxState` SHALL be FX-thread code (bindings' `computeValue`, `performUpdate` merge loop, property-table cell renderers)

### Requirement: Sparse-delta update dispatch

`ObservableEntityList.onUpdate` SHALL capture a `StateDelta` sized to the changed-field count via `entity.getState().captureChanged(fieldPaths, num)` instead of a full `state.copy()`. The scheduled FX action SHALL receive the changed `FieldPath[]` and the `StateDelta`, merging each field into the entity's `fxState`.

#### Scenario: Allocation is proportional to changed-field count

- **WHEN** an entity updates three fields in one tick
- **THEN** the captured delta's backing storage SHALL be sized to three fields, not to the entity's full field count

#### Scenario: Full copy remains for create and count-change events

- **WHEN** `@OnEntityCreated` or `@OnEntityPropertyCountChanged` fires
- **THEN** a full state snapshot SHALL be captured (these events are lifecycle-scoped / rare, not per-tick hot-path)

### Requirement: Bindings read from fxState

`ObservableEntityPropertyBinding.computeValue()` SHALL read its value from the owning `ObservableEntity.fxState` rather than from a per-update snapshot. Bindings SHALL be invalidated when any `FieldPath` in the `changed` array of an update matches their field path.

#### Scenario: Binding sees merged value after an update

- **WHEN** an update changes `fpA` from `10` to `42` and the merge completes on the FX thread
- **AND** a binding for `fpA` is invalidated and re-reads
- **THEN** `computeValue()` SHALL return the `ObservableEntityProperty` reflecting value `42`

#### Scenario: Binding is not invalidated for unrelated fields

- **WHEN** an update changes `fpA` but not `fpB`
- **THEN** a binding for `fpB` SHALL NOT be invalidated by this update
