package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.DoubleBinding;
import javafx.scene.shape.Rectangle;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.model.Vector;

public class HeroIcon extends EntityIcon<Rectangle> {

    private final Rectangle shape;

    public HeroIcon(ObservableEntity oe, DoubleBinding x, DoubleBinding y) {
        super(oe, x, y);

        shape = new Rectangle(140, 140);
        shape.xProperty().bind(x.subtract(70));
        shape.yProperty().bind(y.subtract(70));
        shape.fillProperty().bind(getPlayerColor());

        final ObservableEntityProperty angRot = oe.getPropertyForFieldPath(oe.getEntity().getDtClass().getFieldPathForName("CBodyComponent.m_angRotation"));
        shape.rotateProperty().bind(new DoubleBinding() {
            {
                bind(angRot.rawProperty());
            }

            @Override
            protected double computeValue() {
                Vector v = (Vector) angRot.getRaw();
                return v.getElement(1);
            }
        });
    }

    @Override
    public Rectangle getShape() {
        return shape;
    }

}
