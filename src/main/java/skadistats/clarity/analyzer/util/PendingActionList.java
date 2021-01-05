package skadistats.clarity.analyzer.util;

import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class PendingActionList {

    private final String name;
    private final ReentrantLock lock = new ReentrantLock();
    private List<Runnable> pendingActions;

    public PendingActionList(String name) {
        this.name = name;
    }

    private void withLock(Runnable action) {
        lock.lock();
        try {
            action.run();
        } finally {
            lock.unlock();
        }
    }

    public void add(Runnable action) {
        withLock(() -> {
            if (pendingActions == null) {
                pendingActions = new ArrayList<>();
            }
            pendingActions.add(action);
        });
    }

    public void schedule() {
        schedule(null, null);
    }

    public void schedule(Runnable before, Runnable after) {
        withLock(() -> {
            if (pendingActions == null) return;
            List<Runnable> actionsToExecute = pendingActions;
            pendingActions = null;
            Platform.runLater(() -> executePendingActions(actionsToExecute, before, after));
        });
    }

    private void executePendingActions(List<Runnable> actionsToExecute, Runnable before, Runnable after) {
        withLock(() -> {
            log.debug("{}: executing {} actions", name, actionsToExecute.size());
            if (before != null) before.run();
            actionsToExecute.forEach(Runnable::run);
            if (after != null) after.run();
        });
    }

}
