package skadistats.clarity.analyzer.main.icon;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Shape;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;
import skadistats.clarity.model.FieldPath;


public abstract class EntityIcon<T extends Shape> {

    protected final ObservableEntity oe;
    protected final DoubleBinding x;
    protected final DoubleBinding y;
    private FieldPath m_iTeamNum;

    public EntityIcon(ObservableEntity oe, DoubleBinding x, DoubleBinding y) {
        this.oe = oe;
        this.x = x;
        this.y = y;

    }

    public abstract T getShape();

    protected ObjectBinding<Paint> getTeamColor() {
        m_iTeamNum = oe.getEntity().getDtClass().getFieldPathForName("m_iTeamNum");
        if (m_iTeamNum == null) {
            return new ObjectBinding<Paint>() {
                @Override
                protected Paint computeValue() {
                    return Color.BLACK;
                }
            };
        } else {
            final ObservableEntityProperty teamNumProperty = oe.getPropertyForFieldPath(m_iTeamNum);
            return new ObjectBinding<Paint>() {
                {
                    bind(teamNumProperty.rawProperty());
                }

                @Override
                protected Paint computeValue() {
                    int teamNum = (int) teamNumProperty.getRaw();
                    switch (teamNum) {
                        case 2:
                            return Color.GREEN;
                        case 3:
                            return Color.RED;
                        default:
                            return Color.GRAY;
                    }
                }
            };
        }
    }

    protected ObjectBinding<Paint> getPlayerColor() {
        ObservableEntityProperty id = oe.getPropertyForFieldPath(oe.getEntity().getDtClass().getFieldPathForName("m_iPlayerID"));
        return new ObjectBinding<Paint>() {
            {
                bind(id.rawProperty());
            }
            @Override
            protected Paint computeValue() {
                int n = (int) id.getRaw();
                if (n < 0 || n > 9) {
                    return Color.WHITE;
                } else {
                    return COLORS[n];
                }

            }
        };
    }

    private final Color[] COLORS = {
            Color.web("#3272f6"),
            Color.web("#62f1b5"),
            Color.web("#ba01ba"),
            Color.web("#f1ee0b"),
            Color.web("#ee6603"),
            Color.web("#f281ba"),
            Color.web("#99ab44"),
            Color.web("#64d7f4"),
            Color.web("#037d21"),
            Color.web("#9e6601")
    };

}
