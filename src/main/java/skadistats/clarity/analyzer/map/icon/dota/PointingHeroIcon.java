package skadistats.clarity.analyzer.map.icon.dota;

import javafx.beans.binding.IntegerBinding;
import javafx.scene.shape.Polygon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import static javafx.beans.binding.Bindings.selectInteger;

public class PointingHeroIcon extends EntityIcon<Polygon> {

    private final Polygon shape;

    public PointingHeroIcon(PositionBinder pb, ObservableEntity oe, IntegerBinding playerSlot) {
        super(pb, oe);

        shape = new Polygon(
            0, -200, -120, 200, 120, 200
        );

        shape.fillProperty().bind(getPlayerColorForSlot(playerSlot));

        shape.translateXProperty().bind(getMapX());
        shape.translateYProperty().bind(getMapY());
        shape.rotateProperty().bind(selectInteger(getRotation()));
    }

    @Override
    public Polygon getShape() {
        return shape;
    }

}
