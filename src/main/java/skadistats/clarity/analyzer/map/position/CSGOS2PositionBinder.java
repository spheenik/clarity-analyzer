package skadistats.clarity.analyzer.map.position;

import javafx.beans.value.ObservableValue;
import com.tobiasdiez.easybind.EasyBind;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.model.Vector;

public class CSGOS2PositionBinder implements PositionBinder {

    private static final Vector ZERO = new Vector(0.0f, 0.0f);

    public boolean hasPosition(ObservableEntity oe) {
        return oe.getDtClass().getFieldPathForName( "CBodyComponent.m_cellX")  != null;
    }

    @Override
    public ObservableValue<Float> getMapX(ObservableEntity oe) {
        return getPos("X", 1.0f, oe);
    }

    @Override
    public ObservableValue<Float> getMapY(ObservableEntity oe) {
        return getPos("Y", -1.0f, oe);
    }

    private ObservableValue<Float> getPos(String which, float sign, ObservableEntity oe) {
        return EasyBind.combine(
                oe.getPropertyBinding(Integer.class,  "CBodyComponent.m_cell" + which, 127),
                oe.getPropertyBinding(Float.class,  "CBodyComponent.m_vec" + which, 0.0f),
                (cell, vec) -> sign * (cell * 1024.0f + vec)
        );
    }

    @Override
    public ObservableValue<Float> getRotation(ObservableEntity oe) {
        return EasyBind.wrap(oe.getPropertyBinding(Vector.class, "m_angEyeAngles", ZERO))
                .map(v -> v.getElement(1) - 90);
    }

}
