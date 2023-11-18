package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.csgo.PlayerIcon;
import skadistats.clarity.analyzer.map.position.CSGOS1PositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

public class CSGOS1BindingGenerator implements BindingGenerator {

    private final PositionBinder PB_STANDARD;
    public CSGOS1BindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new CSGOS1PositionBinder();
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
