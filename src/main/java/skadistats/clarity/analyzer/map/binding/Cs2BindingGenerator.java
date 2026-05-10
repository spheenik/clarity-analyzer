package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.PlayerIcon;
import skadistats.clarity.analyzer.map.position.Cs2AndDeadlockPositionBinder;
import skadistats.clarity.analyzer.map.position.DeferringPositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

public class Cs2BindingGenerator implements BindingGenerator {

    private final PositionBinder PB_STANDARD;
    private final PositionBinder PB_PLAYER;

    public Cs2BindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new Cs2AndDeadlockPositionBinder();
        PB_PLAYER = new DeferringPositionBinder(entityList, PB_STANDARD, "m_hPawn");
    }

    @Override
    public EntityIcon<?> createEntityIcon(ObservableEntity oe) {
        var name = oe.getDtClass().getDtName();
        if (name.equals("CCSPlayerController")) {
            return new PlayerIcon(PB_PLAYER, oe);
        } else if (name.equals("CCSPlayerPawn")) {
            return null;
        } else if (PB_STANDARD.hasPosition(oe)) {
            return new DefaultIcon(PB_STANDARD, oe);
        }
        return null;
    }

}
