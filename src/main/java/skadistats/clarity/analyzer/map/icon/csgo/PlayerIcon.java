package skadistats.clarity.analyzer.map.icon.csgo;

import javafx.scene.shape.Polygon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import static javafx.beans.binding.Bindings.selectInteger;

public class PlayerIcon extends EntityIcon<Polygon> {

    private final Polygon shape;

    public PlayerIcon(PositionBinder pb, ObservableEntity oe) {
        super(pb, oe);

        shape = new Polygon(
            0, -120, -120, 120, 120, 120
        );

        shape.fillProperty().bind(getTeamColor());

        shape.translateXProperty().bind(getMapX());
        shape.translateYProperty().bind(getMapY());
        shape.rotateProperty().bind(selectInteger(getRotation()));
    }

    @Override
    public Polygon getShape() {
        return shape;
    }

}
