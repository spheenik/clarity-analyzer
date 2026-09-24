package skadistats.clarity.analyzer.map.icon.dota;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import skadistats.clarity.analyzer.map.dota.DotaTrees;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.DOTAS2PositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import java.util.List;

public class TreesIcon extends EntityIcon<Group> {

    private static final double RADIUS = 48;
    private static final Color STANDING = Color.web("#2f5d34");

    private final Group group = new Group();

    public TreesIcon(PositionBinder pb, ObservableEntity oe, List<DotaTrees.Tree> trees) {
        super(pb, oe);
        group.setMouseTransparent(true);
        @SuppressWarnings("unchecked")
        var words = (ObservableValue<Long>[]) new ObservableValue[(trees.size() + 63) / 64];
        for (var w = 0; w < words.length; w++) {
            words[w] = oe.getPropertyBinding(Long.class, String.format("m_bWorldTreeState.%04d", w), 0L);
        }
        for (var i = 0; i < trees.size(); i++) {
            var tree = trees.get(i);
            var circle = new Circle(
                    DOTAS2PositionBinder.mapXForWorld(tree.x()),
                    DOTAS2PositionBinder.mapYForWorld(tree.y()),
                    RADIUS,
                    STANDING
            );
            var word = words[i >> 6];
            var mask = 1L << (i & 63);
            circle.visibleProperty().bind(Bindings.createBooleanBinding(() -> {
                var v = word.getValue();
                return v != null && (v & mask) != 0;
            }, word));
            group.getChildren().add(circle);
        }
    }

    @Override
    public Group getShape() {
        return group;
    }

    @Override
    public boolean isBackground() {
        return true;
    }

}
