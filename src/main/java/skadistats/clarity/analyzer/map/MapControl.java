package skadistats.clarity.analyzer.map;

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
import skadistats.clarity.analyzer.map.binding.BindingGenerator;
import skadistats.clarity.analyzer.map.binding.DotaS1BindingGenerator;
import skadistats.clarity.analyzer.map.binding.DotaS2BindingGenerator;
import skadistats.clarity.analyzer.map.icon.EntityIcon;
import skadistats.clarity.analyzer.replay.ObservableEntity;
import skadistats.clarity.analyzer.replay.ObservableEntityList;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;

import java.util.List;

import static javafx.beans.binding.Bindings.createDoubleBinding;

public class MapControl extends Region {

    private final ObjectProperty<ObservableEntityList> entityList = new SimpleObjectProperty<>();
    private final IconContainer iconContainer;
    private EntityIcon[] mapEntities;
    private BindingGenerator bindingGenerator;

    public MapControl() {
        iconContainer = new IconContainer();
        getChildren().add(iconContainer);
        entityList.addListener(this::onEntityListSet);
    }

    private void onEntityListSet(ObservableValue<? extends ObservableEntityList> observable, ObservableEntityList oldList, ObservableEntityList newList) {
        iconContainer.icons.getChildren().clear();
        bindingGenerator = determineBindingGenerator(newList.getEngineType());
        if (bindingGenerator != null) {
            mapEntities = new EntityIcon[newList.size()];
            newList.addListener(this::onEntityListChanged);
            add(0, newList);
        }
    }

    private BindingGenerator determineBindingGenerator(EngineType engineType) {
        switch (engineType.getId()) {
            case SOURCE1:
                return new DotaS1BindingGenerator();
            case SOURCE2:
                return new DotaS2BindingGenerator();
            default:
                return null;
        }
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
            if (!bindingGenerator.hasPosition("", oe) && !bindingGenerator.hasPosition("dota_commentator_table.", oe)) {
                continue;
            }

            EntityIcon icon = bindingGenerator.createEntityIcon(oe);

            mapEntities[from + i] = icon;
            iconContainer.icons.getChildren().add(icon.getShape());
        }
    }

    private void clear(int from, int to) {
        for (int i = from; i < to; i++) {
            if (mapEntities[i] != null) {
                iconContainer.icons.getChildren().remove(mapEntities[i].getShape());
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
        private final Group icons;

        private final Translate translate = new Translate();
        private final Scale scale = new Scale();

        public IconContainer() {
            background = new Rectangle();
            background.setFill(Color.gray(0.1));
            getChildren().add(background);
            icons = new Group();
            getChildren().add(icons);

            getTransforms().add(scale);
            getTransforms().add(translate);

            icons.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> {
                background.setX(newValue.getMinX());
                background.setY(newValue.getMinY());
                background.setWidth(newValue.getWidth());
                background.setHeight(newValue.getHeight());
            });

            DoubleBinding scaleBinding = createDoubleBinding(
                    () -> {
                        Bounds b = icons.getLayoutBounds();
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
                        Bounds b = icons.getLayoutBounds();
                        return -b.getMinX() + (getWidth() / scaleBinding.get() - b.getWidth()) * 0.5;
                    },
                    layoutBoundsProperty(),
                    scaleBinding
            ));
            translate.yProperty().bind(createDoubleBinding(
                    () -> {
                        Bounds b = icons.getLayoutBounds();
                        return -b.getMinY() + (getHeight() / scaleBinding.get() - b.getHeight()) * 0.5;
                    },
                    layoutBoundsProperty(),
                    scaleBinding
            ));
        }

    }

}
