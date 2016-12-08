package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class CameraIcon extends EntityIcon<Rectangle> {

    private static final int W = 800;
    private static final int H = 450;

    private final Rectangle shape;

    public CameraIcon(ObservableEntity oe, DoubleBinding x, DoubleBinding y) {
        super(oe, x, y);
        shape = new Rectangle(W, H);
        shape.xProperty().bind(x.subtract(W/2));
        shape.yProperty().bind(y.subtract(H*3/2));
        shape.setFill(Color.TRANSPARENT);
        shape.setStrokeWidth(20);
        shape.strokeProperty().bind(getPlayerColor());
    }

    @Override
    public Rectangle getShape() {
        return shape;
    }

}
