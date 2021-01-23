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
import skadistats.clarity.model.Vector;

public class DotaS1BindingGenerator implements BindingGenerator {

    private static final Vector ZERO = new Vector(0.0f, 0.0f);

    @Override
    public boolean hasPosition(String prefix, ObservableEntity oe) {
        return oe.getDtClass().getFieldPathForName(prefix  + "m_cellX")  != null;
    }

    private FloatBinding getPos(String prefix, String which, int idx, float sign, ObservableEntity oe) {
        ObjectBinding<Integer> cellbits = oe.getPropertyBinding(Integer.class, prefix + "m_cellbits" + which, 7);
        ObjectBinding<Integer> cell = oe.getPropertyBinding(Integer.class, prefix + "m_cell" + which, 127);
        ObjectBinding<Vector> vec = oe.getPropertyBinding(Vector.class, prefix +  "m_vecOrigin", ZERO);
        return Bindings.createFloatBinding(
                () -> {
                    float cellWidth = 1 << cellbits.get();
                    return sign * (cell.get() * cellWidth + vec.get().getElement(idx));
                },
                cellbits,
                cell,
                vec
        );
    }

    @Override
    public FloatBinding getMapX(String prefix, ObservableEntity oe) {
        return getPos(prefix, "X", 0, 1.0f, oe);
    }

    @Override
    public FloatBinding getMapY(String prefix, ObservableEntity oe) {
        return getPos(prefix, "Y", 1, -1.0f, oe);
    }

    @Override
    public EntityIcon createEntityIcon(ObservableEntity oe) {
        String name = oe.getDtClass().getDtName();
        if (name.equals("DT_DOTAPlayer")) {
            return new CameraIcon(this, oe, "dota_commentator_table.");
        } else if (name.equals("DT_DOTA_BaseNPC_Barracks")) {
            return new BuildingIcon(this, oe, 250);
        } else if (name.equals("DT_DOTA_BaseNPC_Tower")) {
            return new BuildingIcon(this, oe, 200);
        } else if (name.equals("DT_DOTA_BaseNPC_Building")) {
            return new BuildingIcon(this, oe, 150);
        } else if (name.equals("DT_DOTA_BaseNPC_Fort")) {
            return new BuildingIcon(this, oe, 300);
        } else if (name.startsWith("DT_DOTA_Unit_Hero_")) {
            return new HeroIcon(this, oe);
        } else {
            return new DefaultIcon(this, oe);
        }
    }

}
