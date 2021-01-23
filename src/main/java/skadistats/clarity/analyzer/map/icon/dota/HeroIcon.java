package skadistats.clarity.analyzer.map.icon.dota;

import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.map.binding.BindingGenerator;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class HeroIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public HeroIcon(BindingGenerator bg, ObservableEntity oe) {
        super(bg, oe);

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
