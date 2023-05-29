package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.csgo.PlayerIcon;
import skadistats.clarity.analyzer.map.position.CSGOS2PositionBinder;
import skadistats.clarity.analyzer.map.position.DeferringPositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

public class CSGOS2BindingGenerator implements BindingGenerator {

    private final PositionBinder PB_STANDARD;
    private final PositionBinder PB_PLAYER;

    public CSGOS2BindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new CSGOS2PositionBinder();
        PB_PLAYER = new DeferringPositionBinder(entityList, PB_STANDARD, "m_hPawn");
    }

    @Override
    public EntityIcon createEntityIcon(ObservableEntity oe) {
        String name = oe.getDtClass().getDtName();
        if (name.equals("CCSPlayerController")) {
            return new PlayerIcon(PB_PLAYER, oe);
        } else if (PB_STANDARD.hasPosition(oe)) {
            return new DefaultIcon(PB_STANDARD, oe);
        }
        return null;
    }

}
