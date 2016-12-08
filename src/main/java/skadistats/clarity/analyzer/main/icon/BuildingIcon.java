package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.shape.Rectangle;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class BuildingIcon extends EntityIcon<Rectangle> {

    private final Rectangle shape;

    public BuildingIcon(ObservableEntity oe, DoubleBinding x, DoubleBinding y, int size) {
        super(oe, x, y);
        shape = new Rectangle(size, size);
        shape.xProperty().bind(x.subtract(size/2));
        shape.yProperty().bind(y.subtract(size/2));
        shape.fillProperty().bind(getTeamColor());
    }

    @Override
    public Rectangle getShape() {
        return shape;
    }

}
