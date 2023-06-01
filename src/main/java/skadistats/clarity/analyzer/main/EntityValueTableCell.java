package skadistats.clarity.analyzer.main;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.control.TableCell;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import skadistats.clarity.analyzer.replay.ObservableEntityProperty;

class EntityValueTableCell extends TableCell<ObservableEntityProperty, String> {

    private final Animation animation = new Transition() {
        {
            setCycleDuration(Duration.millis(500));
            setInterpolator(Interpolator.EASE_OUT);
        }

        @Override
        protected void interpolate(double frac) {
            Color col = Color.YELLOW.interpolate(Color.WHITE, frac);
            getTableRow().setStyle(String.format(
                    "-fx-control-inner-background: #%02X%02X%02X;",
                    (int) (col.getRed() * 255),
                    (int) (col.getGreen() * 255),
                    (int) (col.getBlue() * 255)
            ));
        }
    };

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(item);
        ObservableEntityProperty oep = (ObservableEntityProperty) getTableRow().getItem();
        if (oep != null) {
            animation.stop();
            animation.playFrom(Duration.millis(System.currentTimeMillis() - oep.getLastChangedAtMillis()));
        }
    }

}
