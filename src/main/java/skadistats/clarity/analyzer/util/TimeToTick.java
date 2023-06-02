package skadistats.clarity.analyzer.util;

import skadistats.clarity.model.EngineType;

public class TimeToTick {

    public static EngineType engineType;

    public static int secondsToTicks(int seconds) {
        return (int)(seconds * 1000.0f / engineType.getMillisPerTick());
    }

    public static float millisBetweenTicks(int currentTick, int previousTick) {
        return (currentTick - previousTick) * engineType.getMillisPerTick();
    }


}
