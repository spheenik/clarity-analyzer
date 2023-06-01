package skadistats.clarity.analyzer.map.icon;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.LongBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import static javafx.beans.binding.Bindings.selectFloat;
import static javafx.beans.binding.Bindings.selectInteger;
import static javafx.beans.binding.Bindings.selectLong;


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

    private final PositionBinder pb;
    private final ObservableEntity oe;

    public EntityIcon(PositionBinder pb, ObservableEntity oe) {
        this.pb = pb;
        this.oe = oe;
    }

    public abstract T getShape();

    protected FloatBinding getMapX() {
        return selectFloat(pb.getMapX(oe));
    }

    protected FloatBinding getMapY() {
        return selectFloat(pb.getMapY(oe));
    }

    protected FloatBinding getRotation() {
        return selectFloat(pb.getRotation(oe));
    }

    protected IntegerBinding getPlayerId() {
        return selectInteger(oe.getPropertyBinding(Integer.class, "m_iPlayerID", -1));
    }

    protected IntegerBinding getTeamNum() {
        return selectInteger(oe.getPropertyBinding(Integer.class, "m_iTeamNum", 0));
    }

    protected LongBinding getModelHandle() {
        return selectLong(oe.getPropertyBinding(Long.class, "CBodyComponent.m_hModel", 0L));
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
