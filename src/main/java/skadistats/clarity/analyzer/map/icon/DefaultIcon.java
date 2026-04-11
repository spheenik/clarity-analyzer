package skadistats.clarity.analyzer.map.icon;

import javafx.beans.binding.IntegerBinding;
import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class DefaultIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public DefaultIcon(PositionBinder pb, ObservableEntity oe) {
        this(pb, oe, null);
    }

    public DefaultIcon(PositionBinder pb, ObservableEntity oe, IntegerBinding playerSlot) {
        super(pb, oe);
        shape = new Ellipse(60, 60);
        shape.centerXProperty().bind(getMapX());
        shape.centerYProperty().bind(getMapY());
        shape.fillProperty().bind(playerSlot != null ? getPlayerColorOrTeamColor(playerSlot) : getTeamColor());
    }

    @Override
    public Ellipse getShape() {
        return shape;
    }

}
