package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public interface BindingGenerator {

    EntityIcon<?> createEntityIcon(ObservableEntity oe);

}
