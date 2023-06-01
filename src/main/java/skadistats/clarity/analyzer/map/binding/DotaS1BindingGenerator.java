package skadistats.clarity.analyzer.map.binding;

import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.dota.BuildingIcon;
import skadistats.clarity.analyzer.map.icon.dota.CameraIcon;
import skadistats.clarity.analyzer.map.icon.dota.HeroIcon;
import skadistats.clarity.analyzer.map.position.DOTAS1PositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;

public class DotaS1BindingGenerator implements BindingGenerator {

    private final PositionBinder PB_STANDARD;
    private final PositionBinder PB_PLAYER;

    public DotaS1BindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new DOTAS1PositionBinder();
        PB_PLAYER = new DOTAS1PositionBinder("dota_commentator_table.");
    }

    @Override
    public EntityIcon<?> createEntityIcon(ObservableEntity oe) {
        String name = oe.getDtClass().getDtName();
        if (name.equals("DT_DOTAPlayer")) {
            return new CameraIcon(PB_PLAYER, oe);
        } else if (name.equals("DT_DOTA_BaseNPC_Barracks")) {
            return new BuildingIcon(PB_STANDARD, oe, 250);
        } else if (name.equals("DT_DOTA_BaseNPC_Tower")) {
            return new BuildingIcon(PB_STANDARD, oe, 200);
        } else if (name.equals("DT_DOTA_BaseNPC_Building")) {
            return new BuildingIcon(PB_STANDARD, oe, 150);
        } else if (name.equals("DT_DOTA_BaseNPC_Fort")) {
            return new BuildingIcon(PB_STANDARD, oe, 300);
        } else if (name.startsWith("DT_DOTA_Unit_Hero_")) {
            return new HeroIcon(PB_STANDARD, oe);
        } else if (PB_STANDARD.hasPosition(oe)) {
            return new DefaultIcon(PB_STANDARD, oe);
        }
        return null;
    }

}
