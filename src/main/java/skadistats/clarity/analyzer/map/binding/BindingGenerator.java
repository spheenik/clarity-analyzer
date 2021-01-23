package skadistats.clarity.analyzer.map.binding;

import javafx.beans.binding.FloatBinding;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public interface BindingGenerator {

    boolean hasPosition(String prefix, ObservableEntity oe);
    FloatBinding getMapX(String prefix, ObservableEntity oe);
    FloatBinding getMapY(String prefix, ObservableEntity oe);

    EntityIcon createEntityIcon(ObservableEntity oe);

}
