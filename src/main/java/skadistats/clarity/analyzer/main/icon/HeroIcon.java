package skadistats.clarity.analyzer.main.icon;

import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class HeroIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public HeroIcon(ObservableEntity oe) {
        super(oe);

        shape = new Ellipse(140, 140);
        shape.fillProperty().bind(getPlayerColor());
        shape.translateXProperty().bind(getMapX());
        shape.translateYProperty().bind(getMapY());
    }

    @Override
    public Ellipse getShape() {
        return shape;
    }

}
