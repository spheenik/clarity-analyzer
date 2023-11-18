package skadistats.clarity.analyzer.map.position;

import com.tobiasdiez.easybind.EasyBind;
import javafx.beans.value.ObservableValue;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.model.Vector;

public class CSGOS1PositionBinder implements PositionBinder {

    private static final Vector ZERO = new Vector(0.0f, 0.0f);

    private final String prefix;

    public CSGOS1PositionBinder() {
        this("");
    }

    public CSGOS1PositionBinder(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean hasPosition(ObservableEntity oe) {
        return oe.getDtClass().getFieldPathForName(prefix  + "cslocaldata.m_vecOrigin")  != null;
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
        return EasyBind.map(
            oe.getPropertyBinding(Vector.class,  prefix + "cslocaldata.m_vecOrigin", ZERO),
            (vec) -> sign * vec.getElement(idx)
        );
    }

    @Override
    public ObservableValue<Float> getRotation(ObservableEntity oe) {
        return EasyBind.map(
            oe.getPropertyBinding(Float.class,  prefix + "m_angEyeAngles[1]", 0.0f),
            (ang) -> ang
        );
    }

}
