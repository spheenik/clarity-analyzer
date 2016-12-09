package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.shape.Polygon;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.model.Vector;

import java.util.HashMap;
import java.util.Map;

public class PointingHeroIcon extends EntityIcon<Polygon> {

    private final Polygon shape;

    public PointingHeroIcon(ObservableEntity oe) {
        super(oe);

        shape = new Polygon(
            0, -200, -120, 200, 120, 200
        );

        shape.fillProperty().bind(getPlayerColor());

        ObjectBinding<Vector> angRotVector = oe.getPropertyBinding(Vector.class, "CBodyComponent.m_angRotation", null);
        DoubleBinding angRot = Bindings.createDoubleBinding(() -> (double) angRotVector.get().getElement(1), angRotVector);

        IntegerBinding angDiff = Bindings.selectInteger(oe.getPropertyBinding(Integer.class, "m_anglediff", 0));

        shape.translateXProperty().bind(getMapX());
        shape.translateYProperty().bind(getMapY());
        shape.rotateProperty().bind(getBaseAngle().add(angRot).add(angDiff));
    }

    @Override
    public Polygon getShape() {
        return shape;
    }

    private DoubleBinding getBaseAngle() {
        long modelHandle = getModelHandle().get();
        DoubleBinding binding = BASE_ANGLES.get(modelHandle);
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
