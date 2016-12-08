package skadistats.clarity.analyzer.main;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.transform.Scale;
import skadistats.clarity.analyzer.main.icon.BuildingIcon;
import skadistats.clarity.analyzer.main.icon.CameraIcon;
import skadistats.clarity.analyzer.main.icon.DefaultIcon;
import skadistats.clarity.analyzer.main.icon.EntityIcon;
import skadistats.clarity.analyzer.main.icon.HeroIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.List;

public class MapControl extends Pane implements ListChangeListener<ObservableEntity> {

    private static final double MAP_MAX = 29000.0 / 2.0;

    private final Image mapImage;
    private final ImageView background;
    private final Group icons;

    private MapEntity[] mapEntities;

    public MapControl() {
        mapImage = new Image(getClass().getResourceAsStream("/images/minimap_686.jpg"));

        double scale = MAP_MAX / mapImage.getWidth();
        background = new ImageView(mapImage);
        background.getTransforms().add(new Scale(scale, scale));
        getChildren().add(background);

        icons = new Group();
        getChildren().add(icons);
    }

    public double getSize() {
        return MAP_MAX;
    }

    public void setEntities(ObservableList<ObservableEntity> entities) {
        mapEntities = new MapEntity[entities.size()];
        icons.getChildren().clear();
        entities.addListener(this);
        add(0, entities);
    }

    @Override
    public void onChanged(Change<? extends ObservableEntity> change) {
        while(change.next()) {
            if(change.wasPermutated()) {
                clear(change.getFrom(), change.getTo());
                add(change.getFrom(), change.getList().subList(change.getFrom(), change.getTo()));
            } else {
                if(change.wasRemoved()) {
                    clear(change.getFrom(), change.getFrom() + change.getRemovedSize());
                }
                if(change.wasAdded()) {
                    add(change.getFrom(), change.getAddedSubList());
                }
            }
        }
    }

    private void add(int from, List<? extends ObservableEntity> entities) {
        for (int i = 0; i < entities.size(); i++) {
            ObservableEntity oe = entities.get(i);
            Entity e = oe.getEntity();
            if (e == null) {
                continue;
            }
            FieldPath fp = e.getDtClass().getFieldPathForName("CBodyComponent.m_cellX");
            if (fp == null) {
                continue;
            }
            mapEntities[from + i] = new MapEntity(oe, fp);
        }
    }

    private void clear(int from, int to) {
        for (int i = from; i < to; i++) {
            if (mapEntities[i] != null) {
                mapEntities[i].clear();
                mapEntities[i] = null;
            }
        }
    }

    private class MapEntity {

        private final EntityIcon<?> icon;
        private final IntegerProperty cellX = new SimpleIntegerProperty();
        private final IntegerProperty cellY = new SimpleIntegerProperty();
        private final DoubleProperty vecX = new SimpleDoubleProperty();
        private final DoubleProperty vecY = new SimpleDoubleProperty();

        public MapEntity(ObservableEntity oe, FieldPath fp) {
            cellX.bind(oe.getPropertyForFieldPath(fp).<Integer>rawProperty());
            fp.path[fp.last]++;
            cellY.bind(oe.getPropertyForFieldPath(fp).<Integer>rawProperty());
            fp.path[fp.last]++;
            fp.path[fp.last]++;
            vecX.bind(oe.getPropertyForFieldPath(fp).<Double>rawProperty());
            fp.path[fp.last]++;
            vecY.bind(oe.getPropertyForFieldPath(fp).<Double>rawProperty());

            DoubleBinding vx = cellX.multiply(128.0).add(vecX).multiply(1.0).subtract(16384.0).add(MAP_MAX * 0.5);
            DoubleBinding vy = cellY.multiply(128.0).add(vecY).multiply(- 1.0).add(16384.0).add(MAP_MAX * 0.5);

            String name = oe.getEntity().getDtClass().getDtName();
            if (name.equals("CDOTAPlayer")) {
                icon = new CameraIcon(oe, vx, vy);
            } else if (name.equals("CDOTA_BaseNPC_Barracks")) {
                icon = new BuildingIcon(oe, vx, vy, 250);
            } else if (name.equals("CDOTA_BaseNPC_Tower")) {
                icon = new BuildingIcon(oe, vx, vy, 200);
            } else if (name.equals("CDOTA_BaseNPC_Building")) {
                icon = new BuildingIcon(oe, vx, vy, 150);
            } else if (name.equals("CDOTA_BaseNPC_Fort")) {
                icon = new BuildingIcon(oe, vx, vy, 300);
            } else if (name.startsWith("CDOTA_Unit_Hero_")) {
                icon = new HeroIcon(oe, vx, vy);
            } else {
                icon = new DefaultIcon(oe, vx, vy);
            }
            icons.getChildren().add(icon.getShape());
        }

        public void clear() {
            icons.getChildren().remove(icon.getShape());
        }

    }

}
