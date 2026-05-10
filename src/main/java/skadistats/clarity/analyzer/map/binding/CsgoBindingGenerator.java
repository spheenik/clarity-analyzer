package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.PlayerIcon;
import skadistats.clarity.analyzer.map.position.CsgoPositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

public class CsgoBindingGenerator implements BindingGenerator {

    private final PositionBinder PB_STANDARD;
    public CsgoBindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new CsgoPositionBinder();
    }

    @Override
    public EntityIcon<?> createEntityIcon(ObservableEntity oe) {
        var name = oe.getDtClass().getDtName();
        if (name.equals("DT_CSPlayer")) {
            return new PlayerIcon(PB_STANDARD, oe);
        } else if (PB_STANDARD.hasPosition(oe)) {
            return new DefaultIcon(PB_STANDARD, oe);
        }
        return null;
    }

}
