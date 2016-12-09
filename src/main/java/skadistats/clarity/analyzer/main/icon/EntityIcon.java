package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import skadistats.clarity.analyzer.main.MapControl;
import skadistats.clarity.analyzer.replay.ObservableEntity;


public abstract class EntityIcon<T extends Shape> {

    private final Color[] PLAYER_COLORS = {
            Color.web("#3272f6"),
            Color.web("#62f1b5"),
            Color.web("#ba01ba"),
            Color.web("#f1ee0b"),
            Color.web("#ee6603"),
            Color.web("#f281ba"),
            Color.web("#99ab44"),
            Color.web("#64d7f4"),
            Color.web("#037d21"),
            Color.web("#9e6601")
    };

    protected final ObservableEntity oe;

    public EntityIcon(ObservableEntity oe) {
        this.oe = oe;
    }

    public abstract T getShape();

    protected IntegerBinding getCellX() {
        return Bindings.selectInteger(oe.getPropertyBinding(Integer.class, "CBodyComponent.m_cellX", 0));
    }

    protected IntegerBinding getCellY() {
        return Bindings.selectInteger(oe.getPropertyBinding(Integer.class, "CBodyComponent.m_cellY", 0));
    }

    protected DoubleBinding getVecX() {
        return Bindings.selectDouble(oe.getPropertyBinding(Double.class, "CBodyComponent.m_vecX", 0.0));
    }

    protected DoubleBinding getVecY() {
        return Bindings.selectDouble(oe.getPropertyBinding(Double.class, "CBodyComponent.m_vecY", 0.0));
    }

    protected DoubleBinding getWorldX() {
        return getCellX().multiply(128.0).add(getVecX());
    }

    protected DoubleBinding getWorldY() {
        return getCellY().multiply(-128.0).subtract(getVecY()).add(32768.0);
    }

    protected DoubleBinding getMapX() {
        return getWorldX().subtract(16384.0).subtract(MapControl.MIN_X);
    }

    protected DoubleBinding getMapY() {
        return getWorldY().subtract(16384.0).subtract(MapControl.MIN_Y);
    }

    protected IntegerBinding getPlayerId() {
        return Bindings.selectInteger(oe.getPropertyBinding(Integer.class, "m_iPlayerID", -1));
    }

    protected IntegerBinding getTeamNum() {
        return Bindings.selectInteger(oe.getPropertyBinding(Integer.class, "m_iTeamNum", 0));
    }

    protected LongBinding getModelHandle() {
        return Bindings.selectLong(oe.getPropertyBinding(Long.class, "CBodyComponent.m_hModel", 0L));
    }

    protected ObjectBinding<Paint> getTeamColor() {
        IntegerBinding teamNum = getTeamNum();
        return Bindings.createObjectBinding(() -> {
            int n = teamNum.get();
            switch (n) {
                case 2:
                    return Color.GREEN;
                case 3:
                    return Color.RED;
                default:
                    return Color.GRAY;
            }
        }, teamNum);
    }

    protected ObjectBinding<Paint> getPlayerColor() {
        IntegerBinding playerId = getPlayerId();
        return Bindings.createObjectBinding(() -> {
            int n = playerId.get();
            if (n < 0 || n > 9) {
                return Color.WHITE;
            } else {
                return PLAYER_COLORS[n];
            }
        }, playerId);
    }

}
