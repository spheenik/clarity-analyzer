package skadistats.clarity.analyzer.map.icon.dota;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.scene.shape.Polygon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import java.util.HashMap;
import java.util.Map;

import static javafx.beans.binding.Bindings.selectInteger;

public class PointingHeroIcon extends EntityIcon<Polygon> {

    private final Polygon shape;

    public PointingHeroIcon(PositionBinder pb, ObservableEntity oe) {
        super(pb, oe);

        shape = new Polygon(
            0, -200, -120, 200, 120, 200
        );

        shape.fillProperty().bind(getPlayerColor());

        var angDiff = selectInteger(oe.getPropertyBinding(Integer.class, "m_anglediff", 0));

        shape.translateXProperty().bind(getMapX());
        shape.translateYProperty().bind(getMapY());
        shape.rotateProperty().bind(getBaseAngle().add(selectInteger(getRotation())).add(angDiff));
    }

    @Override
    public Polygon getShape() {
        return shape;
    }

    private DoubleBinding getBaseAngle() {
        var modelHandle = getModelHandle().get();
        var binding = BASE_ANGLES.get(modelHandle);
        if (binding != null) {
            return binding;
        }
        return new DoubleBinding() {
            @Override
            protected double computeValue() {
                return 0.0;
            }
        };
    }

    private static final Map<Long, DoubleBinding> BASE_ANGLES = new HashMap<>();

}
