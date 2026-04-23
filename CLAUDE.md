# CLAUDE.md — clarity-analyzer

JavaFX application for interactively inspecting Dota 2 / CS:GO / CS2 /
Deadlock replays. Primary downstream consumer of `clarity`.

## Build / run
```bash
./gradlew build
./gradlew packageUnoJar       # fat jar → build/libs/clarity-analyzer.jar
./gradlew run                 # launch the GUI
```

Java **21** toolchain. JavaFX 21 via the `org.openjfx.javafxplugin`.
Uses **Lombok** via
`io.freefair.lombok` — be aware when editing (annotations generate
getters/setters/builders).

Main class: `skadistats.clarity.analyzer.AnalyzerLauncher`.

## Layout gotchas
- Package task is `packageUnoJar` (not `Package` like in
  `clarity-examples`) — different uno-jar plugin config.
- JavaFX graphics runtime jars are pulled for **all three platforms**
  (win/linux/mac) via `runtimeOnly`, so the uno-jar is cross-platform.
- FXML lives under `src/main/resources/fx/main.fxml`. Controllers
  referenced from FXML are wired by name — renames need to happen in
  both places.
- Don't launch the GUI just to verify a change — compile-only checks
  are expected (see memory).

## Source layout
```
src/main/java/skadistats/clarity/analyzer/
├── AnalyzerLauncher.java   entry point
├── Analyzer.java
├── main/    MainView, ReplayController, NavigationController, table cells
├── replay/  ObservableEntity{,List,Property}, PropertySupportRunner
├── map/     MapControl + position/icon/binding helpers
└── util/
```

## Upstream
- clarity: `/home/spheenik/projects/clarity/clarity`. Wired in as a
  Gradle **composite build** via `includeBuild("../clarity")` in
  `settings.gradle.kts` (conditional on the sibling checkout existing).
  Gradle substitutes the declared `com.skadistats:clarity:<version>`
  dependency with the sibling project — no publish step needed.
  Keep the pinned version string in sync with the parser's `version`
  in `build.gradle.kts` so standalone builds (without the sibling
  checkout) fall back to a sensible Maven Central coordinate.
- clarity-protobuf flows in transitively via clarity; don't depend on
  it directly.

## FX-side entity state model
`ObservableEntity.fxState` is a persistent `EntityState` owned by the
FX thread. Seeded from a full parse-side snapshot on create; per-tick
updates arrive as sparse `StateDelta`s and are merged in place via
`EntityState.applyFrom`. No per-tick full-state copy. Parse thread
never reads or writes `fxState` — only the immutable delta crosses the
thread boundary (via the pending-action queue).

Primitive binding accessors on `ObservableEntity` —
`getIntPropertyBinding(name, int default)`,
`getLongPropertyBinding(name, long default)`,
`getFloatPropertyBinding(name, float default)` — produce JavaFX
primitive-specialized `ReadOnly{Integer,Long,Float}Property` that read
off `fxState` through clarity's primitive state accessors, no boxing
at the value-read boundary. The generic
`getPropertyBinding(Class<T>, name, default)` stays for `Vector` and
other reference types.
