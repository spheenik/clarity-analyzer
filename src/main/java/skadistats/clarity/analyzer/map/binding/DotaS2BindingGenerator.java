package skadistats.clarity.analyzer.map.binding;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.FloatBinding;
import javafx.beans.binding.ObjectBinding;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.dota.BuildingIcon;
import skadistats.clarity.analyzer.map.icon.dota.CameraIcon;
import skadistats.clarity.analyzer.map.icon.dota.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.dota.HeroIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;

public class DotaS2BindingGenerator implements BindingGenerator {

    @Override
    public boolean hasPosition(String prefix, ObservableEntity oe) {
        return oe.getDtClass().getFieldPathForName(prefix + "CBodyComponent.m_cellX")  != null;
    }

    private FloatBinding getPos(String prefix, String which, float sign, ObservableEntity oe) {
        ObjectBinding<Integer> cell = oe.getPropertyBinding(Integer.class, prefix + "CBodyComponent.m_cell" + which, 127);
        ObjectBinding<Float> vec = oe.getPropertyBinding(Float.class, prefix + "CBodyComponent.m_vec" + which, 0.0f);
        return Bindings.createFloatBinding(
                () -> sign * (cell.get() * 128.0f + vec.get()),
                cell,
                vec
        );
    }

    @Override
    public FloatBinding getMapX(String prefix, ObservableEntity oe) {
        return getPos(prefix, "X", 1.0f, oe);
    }

    @Override
    public FloatBinding getMapY(String prefix, ObservableEntity oe) {
        return getPos(prefix, "Y", -1.0f, oe);
    }

    @Override
    public EntityIcon createEntityIcon(ObservableEntity oe) {
        String name = oe.getDtClass().getDtName();
        if (name.equals("CDOTAPlayer")) {
            return new CameraIcon(this, oe, "");
        } else if (name.equals("CDOTA_BaseNPC_Barracks")) {
            return new BuildingIcon(this, oe, 250);
        } else if (name.equals("CDOTA_BaseNPC_Tower")) {
            return new BuildingIcon(this, oe, 200);
        } else if (name.equals("CDOTA_BaseNPC_Building")) {
            return new BuildingIcon(this, oe, 150);
        } else if (name.equals("CDOTA_BaseNPC_Fort")) {
            return new BuildingIcon(this, oe, 300);
        } else if (name.startsWith("CDOTA_Unit_Hero_")) {
            return new HeroIcon(this, oe);
        } else {
            return new DefaultIcon(this, oe);
        }
    }

}
