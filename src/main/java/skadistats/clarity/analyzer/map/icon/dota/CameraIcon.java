package skadistats.clarity.analyzer.map.icon.dota;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import static javafx.beans.binding.Bindings.selectFloat;

public class CameraIcon extends EntityIcon<Polygon> {

    private static final int W = 800;
    private static final int H = 450;

    private final Polygon shape;

    public CameraIcon(PositionBinder pb, ObservableEntity oe) {
        super(pb, oe);

        int w = W/2;
        int h = H/2;
        shape = new Polygon(-w, -h, w, -h, w, h, -w, h);
        shape.setFill(Color.TRANSPARENT);
        shape.setStrokeWidth(20);
        shape.strokeProperty().bind(getPlayerColor());

        shape.translateXProperty().bind(selectFloat(getMapX()).subtract(W/2));
        shape.translateYProperty().bind(selectFloat(getMapY()).subtract(H));
    }

    @Override
    public Polygon getShape() {
        return shape;
    }

}
