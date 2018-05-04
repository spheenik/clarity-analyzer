package skadistats.clarity.analyzer.replay;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Slider;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Main;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.source.LiveSource;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@UsesEntities
public class ReplayController {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Slider slider;
    private ScheduledFuture<?> timer;

    private Property<PropertySupportRunner> runner = new SimpleObjectProperty<>();

    private IntegerBinding tick = Bindings.selectInteger(
            EasyBind.select(runner)
                    .selectObject(PropertySupportRunner::tickProperty)
                    .orElse(0)
    );

    private IntegerBinding lastTick = Bindings.selectInteger(
            EasyBind.select(runner)
                    .selectObject(PropertySupportRunner::lastTickProperty)
                    .orElse(0)
    );

    private BooleanProperty playing = new SimpleBooleanProperty(false);

    public ReplayController(Slider slider) {
        this.slider = slider;
        slider.maxProperty().bind(lastTick);
        playing.addListener(this::playingStateChanged);
        slider.valueProperty().addListener(this::sliderValueChanged);
        tick.addListener(this::tickChanged);
        Main.primaryStage.setOnCloseRequest(event -> haltIfRunning());
    }

    private void playingStateChanged(ObservableValue<? extends Boolean> v, Boolean o, Boolean n) {
        if (n) {
            if (timer == null) {
                timer = executor.scheduleAtFixedRate(
                        this::timerTick,
                        0L,
                        (long)(getRunner().getEngineType().getMillisPerTick() * 1000000.0f),
                        TimeUnit.NANOSECONDS
                );
            }
        } else {
            if (timer != null) {
                timer.cancel(true);
                timer = null;
            }
        }
    }

    private void sliderValueChanged(ObservableValue<? extends Number> v, Number o, Number n) {
        double val = n.doubleValue();
        // Hack: if the value is not exactly an integer, the slider has been clicked
        if (val != Math.floor(val)) {
            getRunner().setDemandedTick(n.intValue());
        }
    }

    private void tickChanged(ObservableValue<? extends Number> v, Number o, Number n) {
        if (!slider.isValueChanging()) {
            slider.setValue(n.doubleValue());
        }
    }

    private void timerTick() {
        Platform.runLater(() -> {
            if (slider.isValueChanging()) {
                return;
            }
            PropertySupportRunner r = getRunner();
            if (r.getTick() < r.getLastTick() && !r.isResetting()) {
                r.setDemandedTick(r.getTick() + 1);
            }
        });
    }

    public ObservableEntityList load(File f) throws IOException {
        haltIfRunning();
        PropertySupportRunner r = new PropertySupportRunner(new LiveSource(f.getAbsoluteFile().toString(), 10, TimeUnit.SECONDS));
        ObservableEntityList observableEntities = new ObservableEntityList(r.getEngineType());
        runner.setValue(r);
        r.runWith(this, observableEntities);
        return observableEntities;
    }

    public void haltIfRunning() {
        setPlaying(false);
        if (getRunner() != null) {
            getRunner().halt();
            try {
                getRunner().getSource().close();
            } catch (IOException e) {
                Util.uncheckedThrow(e);
            }
        }
    }

    public PropertySupportRunner getRunner() {
        return runner.getValue();
    }

    public Property<PropertySupportRunner> runnerProperty() {
        return runner;
    }

    public void setRunner(PropertySupportRunner runner) {
        this.runner.setValue(runner);
    }

    public IntegerBinding lastTickProperty() {
        return lastTick;
    }

    public IntegerBinding tickProperty() {
        return tick;
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

}
