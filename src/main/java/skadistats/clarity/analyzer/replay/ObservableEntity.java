package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableListBase;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ObservableEntity extends ObservableListBase<ObservableEntityProperty> {

    private final Entity entity;
    private final StringProperty index;
    private final StringProperty name;

    private List<FieldPath<? super FieldPath>> indices = new ArrayList<>();
    private List<ObservableEntityProperty> properties = new ArrayList<>();
    private boolean changeActive = false;

    public ObservableEntity(Entity entity) {
        this.entity = entity;
        index = new ReadOnlyStringWrapper(String.valueOf(entity.getIndex()));
        name = new ReadOnlyStringWrapper(entity.getDtClass().getDtName());
        Iterator<FieldPath> iter = entity.getState().fieldPathIterator();
        while (iter.hasNext()) {
            FieldPath fp = iter.next();
            indices.add(fp);
            properties.add(new ObservableEntityProperty(entity, fp));
        }
    }

    public ObservableEntity(int index) {
        this.entity = null;
        this.index = new ReadOnlyStringWrapper(String.valueOf(index));
        this.name = new ReadOnlyStringWrapper("");
    }


    @Override
    public ObservableEntityProperty get(int index) {
        return properties.get(index);
    }

    @Override
    public int size() {
        return indices.size();
    }

    public <F extends FieldPath> int getIndexForFieldPath(F fieldPath) {
        return Collections.binarySearch(indices, fieldPath);
    }

    public ObservableEntityProperty getPropertyForFieldPath(FieldPath fieldPath) {
        return properties.get(getIndexForFieldPath(fieldPath));
    }

    public <T> ObjectBinding<T> getPropertyBinding(Class<T> propertyClass, String property, T defaultValue) {
        FieldPath fp = entity.getDtClass().getFieldPathForName(property);
        if (fp == null) {
            return Bindings.createObjectBinding(() -> defaultValue);
        } else {
            ObjectBinding<T> ob = getPropertyForFieldPath(fp).rawProperty();
            return Bindings.createObjectBinding(() -> ob.get(), ob);
        }
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
                property.rawProperty().invalidate();
                property.setDirty(false);
            }
        }
    }

    void update(FieldPath[] fieldPaths, int num) {
        for (int i = 0; i < num; i++) {
            int idx = getIndexForFieldPath(fieldPaths[i]);
            if (idx < 0) {
                ensureChangeOpen();
                idx = - idx - 1;
                indices.add(idx, fieldPaths[i]);
                properties.add(idx, new ObservableEntityProperty(entity, fieldPaths[i]));
                nextAdd(idx, idx);
            } else {
                properties.get(idx).setDirty(true);
            }
        }
    }

    public String getIndex() {
        return index.get();
    }

    public StringProperty indexProperty() {
        return index;
    }

    public void setIndex(String index) {
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

    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getName(), getIndex());
    }
}
