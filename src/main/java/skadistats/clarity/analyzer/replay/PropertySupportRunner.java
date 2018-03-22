package skadistats.clarity.analyzer.replay;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.Source;

import java.io.IOException;

public class PropertySupportRunner extends ControllableRunner {

    private IntegerBinding tick = Bindings.createIntegerBinding(this::getTick);
    private IntegerBinding lastTick = Bindings.createIntegerBinding(this::getLastTick);

    public PropertySupportRunner(Source source) throws IOException {
        super(source);
        source.notifyOnLastTickChanged(() -> Platform.runLater(this.lastTick::invalidate));
    }

    @Override
    protected void setTick(int tick) {
        super.setTick(tick);
        Platform.runLater(this.tick::invalidate);
    }

    public IntegerBinding tickProperty() {
        return tick;
    }

    public IntegerBinding lastTickProperty() {
        return lastTick;
    }

}
