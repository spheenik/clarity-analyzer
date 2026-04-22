package skadistats.clarity.analyzer.util;

import skadistats.clarity.processor.runner.Context;

public class TickHelper {

    public static Context context;
    public static int currentTick = -1;


    public static int secondsToTicks(int seconds) {
        return (int)(seconds * 1000.0f / context.getMillisPerTick());
    }

    public static float millisSince(int previousTick) {
        return (currentTick - previousTick) * context.getMillisPerTick();
    }

    public static boolean isRecent(int previousTick) {
        if (currentTick < previousTick) return false; // when rewinding the replay
        return millisSince(previousTick) <= 5000.0f;
    }


}
