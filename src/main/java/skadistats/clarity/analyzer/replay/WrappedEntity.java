package skadistats.clarity.analyzer.replay;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WrappedEntity {

    private final Entity entity;
    private ObservableList<EntityProperty> properties;
    private Map<FieldPath, Integer> indices = new HashMap<>();

    public WrappedEntity(Entity entity) {
        this.entity = entity;
        properties = FXCollections.observableArrayList();
        List<FieldPath> fieldPaths = entity.getDtClass().collectFieldPaths(entity.getState());
        for (FieldPath fieldPath : fieldPaths) {
            indices.put(fieldPath, properties.size());
            properties.add(new EntityProperty(fieldPath));
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public ObservableList<EntityProperty> getProperties() {
        return properties;
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, String>, ObservableValue<String>> getIndexCellFactory() {
        return param -> new ReadOnlyStringWrapper(
            param.getValue().fp.toString()
        );
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, String>, ObservableValue<String>> getNameCellFactory() {
        return param -> new ReadOnlyStringWrapper(
            entity.getDtClass().getNameForFieldPath(param.getValue().fp)
        );
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, String>, ObservableValue<String>> getValueCellFactory() {
        return param -> param.getValue();
    }

    public void fireUpdates(FieldPath[] fieldPaths, int num) {
        if (properties == null) {
            return;
        }
        for (int ci = 0; ci < num; ci++) {
            Integer pi = indices.get(fieldPaths[ci]);
            if (pi == null) {
                pi = properties.size();
                indices.put(fieldPaths[ci], pi);
                properties.add(new EntityProperty(fieldPaths[ci]));
            } else {
                EntityProperty ep = properties.get(pi);
                ep.fire();
            }
        }
    }

    public class EntityProperty extends ObservableValueBase<String> {

        private final FieldPath fp;

        private EntityProperty(FieldPath fp) {
            this.fp = fp;
        }

        @Override
        public String getValue() {
            return entity.getDtClass().getValueForFieldPath(fp, entity.getState()).toString();
        }

        private void fire() {
            fireValueChangedEvent();
        }

    }
}
