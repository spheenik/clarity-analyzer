package skadistats.clarity.analyzer.map.position;

import javafx.beans.value.ObservableValue;
import com.tobiasdiez.easybind.EasyBind;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

import java.util.function.Function;

public class DeferringPositionBinder implements PositionBinder {

    private final ObservableEntityList entityList;
    private final PositionBinder delegate;
    private final String handleProperty;

    public DeferringPositionBinder(ObservableEntityList entityList, PositionBinder delegate, String handleProperty) {
        this.entityList = entityList;
        this.delegate = delegate;
        this.handleProperty = handleProperty;
    }

    private <T> ObservableValue<T> callOnChild(ObservableEntity parent, Function<ObservableEntity, ObservableValue<T>> fn) {
        return EasyBind.wrap(parent.getPropertyBinding(Integer.class, handleProperty, null))
                .map(entityList::byHandle)
                .flatMap(fn);
    }

    @Override
    public boolean hasPosition(ObservableEntity oe) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ObservableValue<Float> getMapX(ObservableEntity oe) {
        return callOnChild(oe, delegate::getMapX);
    }

    @Override
    public ObservableValue<Float> getMapY(ObservableEntity oe) {
        return callOnChild(oe, delegate::getMapY);
    }

    @Override
    public ObservableValue<Float> getRotation(ObservableEntity oe) {
        return callOnChild(oe, delegate::getRotation);
    }

}
