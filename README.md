# Clarity Analyzer

JavaFX-Application to interactively visualize the raw data of a Dota 2, CSGO, CS2 or Deadlock replay.

![Clarity Analyzer](/screenshot.png?raw=true)

# Requirements

JDK version 17 and above

# Building

Depending on your OS, issue the following command in the base project folder

#### Windows
`gradlew.bat packageUnoJar`

#### Linux / Mac
`./gradlew packageUnoJar`

# Running

## Run from gradle
If you do this, you can skip the building step. From the base project folder:

#### Windows
`gradlew.bat run`

#### Linux / Mac
`./gradlew run`

## Run from jar
You need to build first. Then, from the base project folder:

#### Windows
`java -jar build\libs\clarity-analyzer.jar`

#### Linux / Mac
`java -jar build/libs/clarity-analyzer.jar`

# License

See ![LICENSE](/LICENSE) in the project root.
