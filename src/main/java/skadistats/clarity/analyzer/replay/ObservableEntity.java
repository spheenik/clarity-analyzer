package skadistats.clarity.analyzer.replay;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableListBase;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ObservableEntity extends ObservableListBase<ObservableEntityProperty> {

    private final Entity entity;
    private final IntegerProperty index;
    private final StringProperty name;

    private List<FieldPath> indices = new LinkedList<>();
    private List<ObservableEntityProperty> properties = new LinkedList<>();
    private boolean changeActive = false;

    public ObservableEntity(Entity entity) {
        this.entity = entity;
        index = new ReadOnlyIntegerWrapper(entity.getIndex());
        name = new ReadOnlyStringWrapper(entity.getDtClass().getDtName());
        List<FieldPath> fieldPaths = entity.getDtClass().collectFieldPaths(entity.getState());
        for (FieldPath fieldPath : fieldPaths) {
            indices.add(fieldPath);
            properties.add(new ObservableEntityProperty(entity, fieldPath));
        }
    }

    @Override
    public ObservableEntityProperty get(int index) {
        return properties.get(index);
    }

    @Override
    public int size() {
        return indices.size();
    }

    private int getIndexForFieldPath(FieldPath fieldPath) {
        return Collections.binarySearch(indices, fieldPath);
    }

    private void ensureChangeOpen() {
        if (!changeActive) {
            changeActive = true;
            beginChange();
        }
    }

    void commitChange() {
        if (changeActive) {
            changeActive = false;
            endChange();
        }
        for (ObservableEntityProperty property : properties) {
            if (property.isDirty()) {
                property.valueProperty().invalidate();
                property.setDirty(false);
            }
        }
    }

    void update(FieldPath[] fieldPaths, int num) {
        for (int i = 0; i < num; i++) {
            int idx = getIndexForFieldPath(fieldPaths[i]);
            if (idx < 0) {
                ensureChangeOpen();
                idx = 1 - idx;
                indices.add(idx, fieldPaths[i]);
                properties.add(new ObservableEntityProperty(entity, fieldPaths[i]));
                nextAdd(idx, idx);
            } else {
                properties.get(idx).setDirty(true);
            }
        }
    }

    public int getIndex() {
        return index.get();
    }

    public IntegerProperty indexProperty() {
        return index;
    }

    public void setIndex(int index) {
        this.index.set(index);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

}
