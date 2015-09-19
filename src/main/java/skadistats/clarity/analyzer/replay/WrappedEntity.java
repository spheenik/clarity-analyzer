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
import java.util.Map;

public class WrappedEntity {

    private final Entity entity;
    private Map<FieldPath, EntityProperty> propertyMap = new HashMap<>();
    private ObservableList<Map.Entry<FieldPath, EntityProperty>> propertyList = FXCollections.observableArrayList(propertyMap.entrySet());

    public WrappedEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public ObservableList<Map.Entry<FieldPath, EntityProperty>> getProperties() {
        return propertyList;
    }

    public Callback<TableColumn.CellDataFeatures<Map.Entry<FieldPath, EntityProperty>, String>, ObservableValue<String>> getIndexCellFactory() {
        return param -> new ReadOnlyStringWrapper(
            param.getValue().getKey().toString()
        );
    }

    public Callback<TableColumn.CellDataFeatures<Map.Entry<FieldPath, EntityProperty>, String>, ObservableValue<String>> getNameCellFactory() {
        return param -> new ReadOnlyStringWrapper(
            entity.getDtClass().getNameForFieldPath(param.getValue().getKey())
        );
    }

    public Callback<TableColumn.CellDataFeatures<Map.Entry<FieldPath, EntityProperty>, String>, ObservableValue<String>> getValueCellFactory() {
        return param -> param.getValue().getValue();
    }

    public void fireUpdates(FieldPath[] fieldPaths, int num) {
        if (propertyList == null) {
            return;
        }
        for (int ci = 0; ci < num; ci++) {
            EntityProperty ep = propertyMap.get(fieldPaths[ci]);
            if (ep == null) {
                // TODO: add and fire
            } else {
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
            return String.valueOf(entity.getDtClass().getValueForFieldPath(fp, entity.getState()));
        }

        private void fire() {
            fireValueChangedEvent();
        }

    }
}
