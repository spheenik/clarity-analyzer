package skadistats.clarity.analyzer.replay;

import com.tobiasdiez.easybind.EasyBind;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Slider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.Analyzer;
import skadistats.clarity.analyzer.main.ExceptionDialog;
import skadistats.clarity.analyzer.util.TickHelper;
import skadistats.clarity.io.Util;
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
        var t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Slider slider;
    private ScheduledFuture<?> timer;

    private Property<PropertySupportRunner> runner = new SimpleObjectProperty<>();

    private IntegerBinding tick =
            Bindings.selectInteger(
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
    private ObjectProperty<ObservableEntityList> entityList = new SimpleObjectProperty<>();

    public ReplayController(Slider slider) {
        this.slider = slider;
        slider.maxProperty().bind(lastTick);
        playing.addListener(this::playingStateChanged);
        slider.valueProperty().addListener(this::sliderValueChanged);
        tick.addListener(this::tickChanged);
        Analyzer.primaryStage.setOnCloseRequest(event -> haltIfRunning());
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
        var val = n.doubleValue();
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
            var r = getRunner();
            if (r.getTick() < r.getLastTick() && !r.isResetting()) {
                r.setDemandedTick(r.getTick() + 1);
            }
        });
    }

    public void load(File f) {
        try {
            haltIfRunning();
            var r = new PropertySupportRunner(new LiveSource(f.getAbsoluteFile().toString(), 30, TimeUnit.SECONDS));
            TickHelper.engineType = r.getEngineType();
            var observableEntities = new ObservableEntityList(r.getEngineType());
            runner.setValue(r);
            r.runWith(this, observableEntities);
            entityList.set(observableEntities);
        } catch (Exception e) {
            e.printStackTrace();
            new ExceptionDialog(e).show();
        }
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

    public IntegerBinding lastTickProperty() {
        return lastTick;
    }

    public IntegerBinding tickProperty() {
        return tick;
    }

    public BooleanProperty playingProperty() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing.set(playing);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public ObservableEntityList getEntityList() {
        return entityList.get();
    }

    public ObjectProperty<ObservableEntityList> entityListProperty() {
        return entityList;
    }

}
