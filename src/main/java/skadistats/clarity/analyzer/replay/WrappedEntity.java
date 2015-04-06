package skadistats.clarity.analyzer.replay;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import skadistats.clarity.model.Entity;

public class WrappedEntity {

    private final Entity entity;
    private ObservableList<EntityProperty> properties;

    public WrappedEntity(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }

    public ObservableList<EntityProperty> getProperties() {
        if (properties == null) {
            properties = FXCollections.observableArrayList();
            for (int i = 0; i < entity.getDtClass().getReceiveProps().length; i++) {
                properties.add(new EntityProperty(i));
            }
        }
        return properties;
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, Integer>, ObservableValue<Integer>> getIndexCellFactory() {
        return param -> new ReadOnlyIntegerWrapper(param.getValue().rpIndex).asObject();
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, String>, ObservableValue<String>> getNameCellFactory() {
        return param -> new ReadOnlyStringWrapper(entity.getDtClass().getReceiveProps()[param.getValue().rpIndex].getVarName());
    }

    public Callback<TableColumn.CellDataFeatures<EntityProperty, String>, ObservableValue<String>> getValueCellFactory() {
        return param -> param.getValue();
    }

    public void fireUpdates(int[] indices, int num) {
        if (properties == null) {
            return;
        }
        for (int ci = 0; ci < num; ci++) {
            int o = indices[ci];
            properties.get(o).fire();
        }
    }

    public class EntityProperty extends ObservableValueBase<String> {
        private final int rpIndex;
        private EntityProperty(int rpIndex) {
            this.rpIndex = rpIndex;
        }
        @Override
        public String getValue() {
            return String.valueOf(entity.getState()[rpIndex]);
        }
        private void fire() {
            fireValueChangedEvent();
        }
    }
}
