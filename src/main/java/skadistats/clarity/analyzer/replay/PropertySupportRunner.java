package skadistats.clarity.analyzer.replay;

import javafx.application.Platform;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.Source;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

public class PropertySupportRunner extends ControllableRunner {

    private final PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public PropertySupportRunner(Source s) throws IOException {
        super(s);
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        changes.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        changes.removePropertyChangeListener(l);
    }


    private final ReentrantLock notificationLock = new ReentrantLock();
    private int notificationOldTick = Integer.MIN_VALUE;
    private int notificationTick = 0;

    private void commitNotification() {
        notificationLock.lock();
        try {
            changes.firePropertyChange("tick", notificationOldTick, notificationTick);
            notificationOldTick = Integer.MIN_VALUE;
        } finally {
            notificationLock.unlock();
        }
    }

    @Override
    protected void setTick(int tick) {
        notificationLock.lock();
        try {
            if (notificationOldTick != Integer.MIN_VALUE) {
                super.setTick(tick);
                notificationTick = tick;
            } else {
                notificationOldTick = getTick();
                super.setTick(tick);
                notificationTick = tick;
                Platform.runLater(this::commitNotification);
            }
        } finally {
            notificationLock.unlock();
        }
    }

}
