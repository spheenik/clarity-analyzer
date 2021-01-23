package skadistats.clarity.analyzer.map.icon.dota;

import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.map.binding.BindingGenerator;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class DefaultIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public DefaultIcon(BindingGenerator bg, ObservableEntity oe) {
        super(bg, oe);
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
