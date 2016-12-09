package skadistats.clarity.analyzer.main.icon;

import javafx.scene.shape.Rectangle;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class BuildingIcon extends EntityIcon<Rectangle> {

    private final Rectangle shape;

    public BuildingIcon(ObservableEntity oe, int size) {
        super(oe);
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
