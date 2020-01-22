package skadistats.clarity.analyzer.main;

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

    public static final double MIN_X = -7500;
    public static final double MAX_X = 7500;
    public static final double MIN_Y = -7400;
    public static final double MAX_Y = 7200;

    private final Image mapImage;
    private final ImageView background;
    private final Group icons;

    private EntityIcon[] mapEntities;

    public MapControl() {
        mapImage = new Image(getClass().getResourceAsStream("/images/Minimap_7.23_Simple.png"));

        double scale = getSize() / mapImage.getWidth();
        background = new ImageView(mapImage);
        background.getTransforms().add(new Scale(scale, scale));
        getChildren().add(background);

        icons = new Group();
        getChildren().add(icons);
    }

    public double getSize() {
        return MAX_Y - MIN_Y;
    }

    public void setEntities(ObservableList<ObservableEntity> entities) {
        mapEntities = new EntityIcon[entities.size()];
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

            String name = oe.getEntity().getDtClass().getDtName();
            EntityIcon icon;
            if (name.equals("CDOTAPlayer")) {
                icon = new CameraIcon(oe);
            } else if (name.equals("CDOTA_BaseNPC_Barracks")) {
                icon = new BuildingIcon(oe, 250);
            } else if (name.equals("CDOTA_BaseNPC_Tower")) {
                icon = new BuildingIcon(oe, 200);
            } else if (name.equals("CDOTA_BaseNPC_Building")) {
                icon = new BuildingIcon(oe, 150);
            } else if (name.equals("CDOTA_BaseNPC_Fort")) {
                icon = new BuildingIcon(oe, 300);
            } else if (name.startsWith("CDOTA_Unit_Hero_")) {
                icon = new HeroIcon(oe);
            } else {
                icon = new DefaultIcon(oe);
            }

            mapEntities[from + i] = icon;
            icons.getChildren().add(icon.getShape());
        }
    }

    private void clear(int from, int to) {
        for (int i = from; i < to; i++) {
            if (mapEntities[i] != null) {
                icons.getChildren().remove(mapEntities[i].getShape());
                mapEntities[i] = null;
            }
        }
    }

}
