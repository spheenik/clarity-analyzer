package skadistats.clarity.analyzer.util;

import skadistats.clarity.model.EngineType;

public class TickHelper {

    public static EngineType engineType;
    public static int currentTick = -1;


    public static int secondsToTicks(int seconds) {
        return (int)(seconds * 1000.0f / engineType.getMillisPerTick());
    }

    public static float millisSince(int previousTick) {
        return (currentTick - previousTick) * engineType.getMillisPerTick();
    }

    public static boolean isRecent(int previousTick) {
        if (currentTick < previousTick) return false; // when rewinding the replay
        return millisSince(previousTick) <= 5000.0f;
    }


}
