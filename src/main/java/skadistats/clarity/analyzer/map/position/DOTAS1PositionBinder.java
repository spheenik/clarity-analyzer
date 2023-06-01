package skadistats.clarity.analyzer.map.position;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import com.tobiasdiez.easybind.EasyBind;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.model.Vector;

import static javafx.beans.binding.Bindings.createObjectBinding;

public class DOTAS1PositionBinder implements PositionBinder {

    private static final Vector ZERO = new Vector(0.0f, 0.0f);

    private final String prefix;

    public DOTAS1PositionBinder() {
        this("");
    }

    public DOTAS1PositionBinder(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean hasPosition(ObservableEntity oe) {
        return oe.getDtClass().getFieldPathForName(prefix  + "m_cellX")  != null;
    }

    @Override
    public ObservableValue<Float> getMapX(ObservableEntity oe) {
        return getPos("X", 0, 1.0f, oe);
    }

    @Override
    public ObservableValue<Float> getMapY(ObservableEntity oe) {
        return getPos("Y", 1, -1.0f, oe);
    }

    private ObservableValue<Float> getPos(String which, int idx, float sign, ObservableEntity oe) {
        return EasyBind.combine(
                oe.getPropertyBinding(Integer.class,  prefix + "m_cellbits" + which, 7),
                oe.getPropertyBinding(Integer.class,  prefix + "m_cell" + which, 127),
                oe.getPropertyBinding(Vector.class,   prefix + "m_vecOrigin", ZERO),
                (cellBits, cell, vec) -> sign * (cell * (1 << cellBits) + vec.getElement(idx))
        );
    }

    @Override
    public ObservableValue<Float> getRotation(ObservableEntity oe) {
        return createObjectBinding(() -> 0.0f);
    }

}
