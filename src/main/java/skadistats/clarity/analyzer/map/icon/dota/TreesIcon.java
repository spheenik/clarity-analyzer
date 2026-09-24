package skadistats.clarity.analyzer.map.icon.dota;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.analyzer.map.dota.DotaTrees;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.position.DOTAS2PositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;

import java.util.List;

/**
 * Draws the world trees of a Dota 2 replay, each visible while its bit in {@code m_bWorldTreeState} is set.
 *
 * <p>Bits below {@link #WORLD_TREE_SLOTS} are world trees; higher bits are used for other trees and ignored.
 * One server protocol can span several map revisions, but only the latest revision's table is available,
 * so the table is checked against the replay and no trees are drawn if it does not fit:
 * <ul>
 * <li>world-tree bits beyond the table are set (the replay has more trees, or it is an older replay that
 * starts with every slot set, whose tree order does not match the map tables)</li>
 * <li>trees are standing but the last {@link #TAIL_SLOTS} slots of the table are all unset (the table has
 * more trees than the replay)</li>
 * </ul>
 */
public class TreesIcon extends EntityIcon<Group> {

    private static final Logger log = LoggerFactory.getLogger(TreesIcon.class);
    private static final int WORLD_TREE_SLOTS = 8192;
    private static final int TAIL_SLOTS = 8;
    private static final double RADIUS = 48;
    private static final Color STANDING = Color.web("#2f5d34");

    private final Group group = new Group();
    private final SimpleBooleanProperty layoutMismatch = new SimpleBooleanProperty(false);

    public TreesIcon(PositionBinder pb, ObservableEntity oe, List<DotaTrees.Tree> trees) {
        super(pb, oe);
        group.setMouseTransparent(true);
        group.visibleProperty().bind(layoutMismatch.not());

        @SuppressWarnings("unchecked")
        var words = (ObservableValue<Long>[]) new ObservableValue[WORLD_TREE_SLOTS / 64];
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

        var size = trees.size();
        if (size < WORLD_TREE_SLOTS) {
            Runnable check = () -> checkLayout(words, size);
            for (var w = Math.max(0, size - TAIL_SLOTS) >> 6; w < words.length; w++) {
                words[w].addListener(o -> check.run());
            }
            check.run();
        }
    }

    private void checkLayout(ObservableValue<Long>[] words, int size) {
        if (layoutMismatch.get()) return;
        var values = new long[words.length];
        for (var w = 0; w < words.length; w++) {
            var v = words[w].getValue();
            values[w] = v == null ? 0L : v;
        }
        if (!fitsTable(values, size)) {
            log.warn("tree state of the replay does not fit the {} trees of its map table, hiding trees", size);
            layoutMismatch.set(true);
        }
    }

    static boolean fitsTable(long[] words, int size) {
        for (var w = size >> 6; w < WORLD_TREE_SLOTS / 64; w++) {
            var mask = w == size >> 6 ? -1L << (size & 63) : -1L;
            if ((word(words, w) & mask) != 0) {
                return false;
            }
        }
        var anyStanding = false;
        for (var i = 0; i < size && !anyStanding; i++) {
            anyStanding = isSet(words, i);
        }
        if (!anyStanding || size < TAIL_SLOTS) {
            return true;
        }
        for (var i = size - TAIL_SLOTS; i < size; i++) {
            if (isSet(words, i)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSet(long[] words, int i) {
        return (word(words, i >> 6) & (1L << (i & 63))) != 0;
    }

    private static long word(long[] words, int w) {
        return w < words.length ? words[w] : 0L;
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
