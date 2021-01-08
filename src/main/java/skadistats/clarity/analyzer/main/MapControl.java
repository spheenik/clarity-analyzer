package skadistats.clarity.analyzer.main;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import skadistats.clarity.analyzer.main.icon.BuildingIcon;
import skadistats.clarity.analyzer.main.icon.CameraIcon;
import skadistats.clarity.analyzer.main.icon.DefaultIcon;
import skadistats.clarity.analyzer.main.icon.EntityIcon;
import skadistats.clarity.analyzer.main.icon.HeroIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;

import java.util.List;

import static javafx.beans.binding.Bindings.createDoubleBinding;

public class MapControl extends Region {

    private final ObjectProperty<ObservableEntityList> entityList = new SimpleObjectProperty<>();
    private final IconContainer icons;
    private EntityIcon[] mapEntities;

    public MapControl() {
        icons = new IconContainer();
        getChildren().add(icons);
        entityList.addListener(this::onEntityListSet);
    }

    private void onEntityListSet(ObservableValue<? extends ObservableEntityList> observable, ObservableEntityList oldList, ObservableEntityList newList) {
        icons.empty();
        mapEntities = new EntityIcon[newList.size()];
        newList.addListener(this::onEntityListChanged);
        add(0, newList);
    }

    private void onEntityListChanged(ListChangeListener.Change<? extends ObservableEntity> change) {
        while (change.next()) {
            if (change.wasUpdated() || change.wasPermutated() || change.wasReplaced()) {
                clear(change.getFrom(), change.getTo());
                add(change.getFrom(), change.getList().subList(change.getFrom(), change.getTo()));
            } else if (change.wasRemoved()) {
                clear(change.getFrom(), change.getFrom() + change.getRemovedSize());
            } else if (change.wasAdded()) {
                add(change.getFrom(), change.getAddedSubList());
            }
        }
    }

    private void add(int from, List<? extends ObservableEntity> entities) {
        for (int i = 0; i < entities.size(); i++) {
            ObservableEntity oe = entities.get(i);
            DTClass cls = oe.getDtClass();
            if (cls == null) {
                continue;
            }
            FieldPath fp = cls.getFieldPathForName("CBodyComponent.m_cellX");
            if (fp == null) {
                continue;
            }

            String name = oe.getDtClass().getDtName();
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

    public ObservableEntityList getEntityList() {
        return entityList.get();
    }

    public ObjectProperty<ObservableEntityList> entityListProperty() {
        return entityList;
    }

    public class IconContainer extends Group {

        private final Rectangle background;

        private final Translate translate = new Translate();
        private final Scale scale = new Scale();

        public IconContainer() {
            background = new Rectangle();
            background.setFill(Color.gray(0.1));
            getChildren().add(background);

            getTransforms().add(scale);
            getTransforms().add(translate);

            layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                background.setX(newValue.getMinX());
                background.setY(newValue.getMinY());
                background.setWidth(newValue.getWidth());
                background.setHeight(newValue.getHeight());
            });

            DoubleBinding scaleBinding = createDoubleBinding(
                    () -> {
                        Bounds b = getLayoutBounds();
                        return Math.min(getWidth() / b.getWidth(), getHeight() / b.getHeight());
                    },
                    widthProperty(),
                    heightProperty(),
                    layoutBoundsProperty()
            );
            scale.xProperty().bind(scaleBinding);
            scale.yProperty().bind(scaleBinding);

            translate.xProperty().bind(createDoubleBinding(
                    () -> {
                        Bounds b = getLayoutBounds();
                        return -b.getMinX() + (getWidth() / scaleBinding.get() - b.getWidth()) * 0.5;
                    },
                    layoutBoundsProperty(),
                    scaleBinding
            ));
            translate.yProperty().bind(createDoubleBinding(
                    () -> {
                        Bounds b = getLayoutBounds();
                        return -b.getMinY() + (getHeight() / scaleBinding.get() - b.getHeight()) * 0.5;
                    },
                    layoutBoundsProperty(),
                    scaleBinding
            ));
        }

        void empty() {
            getChildren().remove(1, getChildren().size());
        }

    }

}
