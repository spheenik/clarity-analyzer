package skadistats.clarity.analyzer.replay;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanIntegerPropertyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Main;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

@UsesEntities
public class ReplayController {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private Property<PropertySupportRunner> runner = new SimpleObjectProperty<>();
    private IntegerProperty tick = new SimpleIntegerProperty(0);
    private IntegerProperty lastTick = new SimpleIntegerProperty(0);
    private BooleanProperty playing = new SimpleBooleanProperty(false);
    private BooleanProperty changing = new SimpleBooleanProperty(false);

    private Timer timer;

    public ReplayController() {
        changing.addListener((observable1, oldValue1, newValue1) -> log.info("slider drag {}", newValue1));
        playing.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                if (timer == null) {
                    timer = new Timer();
                    timer.scheduleAtFixedRate(new TickingTask(), 0L, 1000L / 30L);
                }
            } else {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
            }
        });
        Main.primaryStage.setOnCloseRequest(event -> haltIfRunning());
    }

    private class TickingTask extends TimerTask {
        @Override
        public void run() {
            if (!changing.get()) {
                if (getTick() >= getLastTick()) {
                    setPlaying(false);
                    return;
                }
                if (!getRunner().isResetting()) {
                    getRunner().setDemandedTick(getTick() + 1);
                }
            }
        }
    }

    public ObservableEntityList load(File f) throws IOException, NoSuchMethodException {
        haltIfRunning();
        PropertySupportRunner r = new PropertySupportRunner(new MappedFileSource(f.getAbsoluteFile()));
        ObservableEntityList observableEntities = new ObservableEntityList(r.getEngineType());
        lastTick.bind(new ReadOnlyJavaBeanIntegerPropertyBuilder().bean(r).name("lastTick").build());
        tick.bind(new ReadOnlyJavaBeanIntegerPropertyBuilder().bean(r).name("tick").build());
        runner.setValue(r);
        r.runWith(this, observableEntities);
        r.setDemandedTick(0);
        return observableEntities;
    }

    public void haltIfRunning() {
        setPlaying(false);
        if (getRunner() != null) {
            getRunner().halt();
            try {
                getRunner().getSource().close();
            } catch (IOException e) {
                throw Util.toClarityException(e);
            }
        }
    }

    public ControllableRunner getRunner() {
        return runner.getValue();
    }

    public Property<PropertySupportRunner> runnerProperty() {
        return runner;
    }

    public void setRunner(PropertySupportRunner runner) {
        this.runner.setValue(runner);
    }

    public int getTick() {
        return tick.get();
    }

    public IntegerProperty tickProperty() {
        return tick;
    }

    public void setTick(int tick) {
        this.tick.set(tick);
    }

    public int getLastTick() {
        return lastTick.get();
    }

    public IntegerProperty lastTickProperty() {
        return lastTick;
    }

    public void setLastTick(int lastTick) {
        this.lastTick.set(lastTick);
    }

    public boolean getPlaying() {
        return playing.get();
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing.set(playing);
    }

    public boolean getChanging() {
        return changing.get();
    }

    public BooleanProperty changingProperty() {
        return changing;
    }

    public void setChanging(boolean changing) {
        this.changing.set(changing);
    }
}
