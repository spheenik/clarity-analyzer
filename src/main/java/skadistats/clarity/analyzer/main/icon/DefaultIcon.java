package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.shape.Ellipse;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class DefaultIcon extends EntityIcon<Ellipse> {

    private final Ellipse shape;

    public DefaultIcon(ObservableEntity oe, DoubleBinding x, DoubleBinding y) {
        super(oe, x, y);
        shape = new Ellipse(60, 60);
        shape.centerXProperty().bind(x);
        shape.centerYProperty().bind(y);
        shape.fillProperty().bind(getTeamColor());
    }

    @Override
    public Ellipse getShape() {
        return shape;
    }

}
