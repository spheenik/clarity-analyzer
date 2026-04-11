package skadistats.clarity.analyzer.map.binding;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import skadistats.clarity.analyzer.map.icon.DefaultIcon;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.map.icon.dota.BuildingIcon;
import skadistats.clarity.analyzer.map.icon.dota.CameraIcon;
import skadistats.clarity.analyzer.map.icon.dota.PointingHeroIcon;
import skadistats.clarity.analyzer.map.position.DOTAS2PositionBinder;
import skadistats.clarity.analyzer.map.position.PositionBinder;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.model.EngineType;

public class DotaS2BindingGenerator implements BindingGenerator {

    private static final int EMPTY_HANDLE = 16777215;

    private final PositionBinder PB_STANDARD;
    private final EngineType engineType;

    private final ObjectProperty<int[]> selectedHeroHandles = new SimpleObjectProperty<>(new int[10]);

    public DotaS2BindingGenerator(ObservableEntityList entityList) {
        PB_STANDARD = new DOTAS2PositionBinder();
        engineType = entityList.getEngineType();
    }

    @Override
    public EntityIcon<?> createEntityIcon(ObservableEntity oe) {
        var name = oe.getDtClass().getDtName();
        if (name.equals("CDOTA_PlayerResource")) {
            bindPlayerResource(oe);
            return null;
        } else if (name.equals("CDOTAPlayer") || name.equals("CDOTAPlayerController")) {
            return new CameraIcon(PB_STANDARD, oe);
        } else if (name.equals("CDOTA_BaseNPC_Barracks")) {
            return new BuildingIcon(PB_STANDARD, oe, 250);
        } else if (name.equals("CDOTA_BaseNPC_Tower")) {
            return new BuildingIcon(PB_STANDARD, oe, 200);
        } else if (name.equals("CDOTA_BaseNPC_Building")) {
            return new BuildingIcon(PB_STANDARD, oe, 150);
        } else if (name.equals("CDOTA_BaseNPC_Fort")) {
            return new BuildingIcon(PB_STANDARD, oe, 300);
        } else if (name.startsWith("CDOTA_Unit_Hero_")) {
            return new PointingHeroIcon(PB_STANDARD, oe, playerSlotFor(oe));
        } else if (PB_STANDARD.hasPosition(oe)) {
            return new DefaultIcon(PB_STANDARD, oe, playerSlotFor(oe));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void bindPlayerResource(ObservableEntity pr) {
        var slotBindings = new ObservableValue[10];
        for (int i = 0; i < 10; i++) {
            slotBindings[i] = pr.getPropertyBinding(
                Integer.class,
                String.format("m_vecPlayerTeamData.%04d.m_hSelectedHero", i),
                0
            );
        }
        selectedHeroHandles.bind(Bindings.createObjectBinding(() -> {
            var handles = new int[10];
            for (int i = 0; i < 10; i++) {
                var h = ((ObservableValue<Integer>) slotBindings[i]).getValue();
                handles[i] = h != null ? h : 0;
            }
            return handles;
        }, slotBindings));
    }

    private IntegerBinding playerSlotFor(ObservableEntity oe) {
        int entityHandle = engineType.handleForIndexAndSerial(oe.getIndex(), oe.getSerial());
        var ownerBinding = oe.getPropertyBinding(Integer.class, "m_hOwnerEntity", EMPTY_HANDLE);

        return Bindings.createIntegerBinding(() -> {
            var handles = selectedHeroHandles.get();
            if (handles == null) return -1;
            // direct match: entity itself is a selected hero
            for (int i = 0; i < handles.length; i++) {
                if (handles[i] == entityHandle) return i;
            }
            // indirect match: entity's owner is a selected hero
            var ownerHandle = ownerBinding.getValue();
            if (ownerHandle != null && ownerHandle != EMPTY_HANDLE) {
                for (int i = 0; i < handles.length; i++) {
                    if (handles[i] == ownerHandle) return i;
                }
            }
            return -1;
        }, selectedHeroHandles, ownerBinding);
    }

}
