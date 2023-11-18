package skadistats.clarity.analyzer.map.position;

import javafx.beans.value.ObservableValue;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public interface PositionBinder {
    boolean hasPosition(ObservableEntity oe);
    ObservableValue<Float> getMapX(ObservableEntity oe);
    ObservableValue<Float> getMapY(ObservableEntity oe);
    ObservableValue<Float> getRotation(ObservableEntity oe);
}
