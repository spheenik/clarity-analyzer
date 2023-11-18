package skadistats.clarity.analyzer.map.icon;

import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class DefaultIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public DefaultIcon(PositionBinder pb, ObservableEntity oe) {
        super(pb, oe);
        shape = new Ellipse(60, 60);
        shape.centerXProperty().bind(getMapX());
        shape.centerYProperty().bind(getMapY());
        shape.fillProperty().bind(getTeamColor());
    }

    @Override
    public Ellipse getShape() {
        return shape;
    }

}
