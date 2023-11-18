package skadistats.clarity.analyzer.map.icon.dota;

import javafx.scene.shape.Rectangle;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class BuildingIcon extends EntityIcon<Rectangle> {

    private final Rectangle shape;

    public BuildingIcon(PositionBinder pb, ObservableEntity oe, int size) {
        super(pb, oe);
        shape = new Rectangle(size, size);
        shape.xProperty().bind(getMapX().subtract(size/2));
        shape.yProperty().bind(getMapY().subtract(size/2));
        shape.fillProperty().bind(getTeamColor());
    }

    @Override
    public Rectangle getShape() {
        return shape;
    }

}
