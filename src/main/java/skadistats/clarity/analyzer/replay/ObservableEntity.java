package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableListBase;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.S2DTClass;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ObservableEntity extends ObservableListBase<ObservableEntityProperty> {

    private final DTClass dtClass;
    private final ReadOnlyIntegerProperty index;
    private final ReadOnlyStringProperty name;

    private final List<ObservableEntityProperty> properties;
    private List<ObservableEntityPropertyBinding> propertyBindings;

    private EntityState state;

    public ObservableEntity(int index) {
        this(index, null, null);
    }

    public ObservableEntity(int index, DTClass dtClass, EntityState state) {
        this.dtClass = dtClass;
        this.index = new ReadOnlyIntegerWrapper(index);
        this.state = state;
        this.propertyBindings = null;
        if (dtClass != null) {
            this.name = new ReadOnlyStringWrapper(dtClass.getDtName());
            this.properties = createProperties(state);
        } else {
            this.name = new ReadOnlyStringWrapper("");
            this.properties = null;
        }
    }

    private List<ObservableEntityProperty> createProperties(EntityState state) {
        List<ObservableEntityProperty> properties = new ArrayList<>();
        Iterator<FieldPath> iter = state.fieldPathIterator();
        while (iter.hasNext()) {
            FieldPath fp = iter.next();
            ObservableEntityProperty property = createProperty(fp);
            properties.add(property);
        }
        return properties;
    }

    private ObservableEntityProperty createProperty(FieldPath fp) {
        String type = dtClass.evaluate(
                s1 -> s1.getReceiveProps()[fp.s1().idx()].getSendProp().getType().toString(),
                s2 -> s2.getTypeForFieldPath(fp.s2()).toString()
        );
        ObservableEntityProperty property = new ObservableEntityProperty(
                fp,
                type,
                dtClass.getNameForFieldPath(fp),
                () -> ObservableEntity.this.state.getValueForFieldPath(fp)
        );
        return property;
    }

    void performUpdate(FieldPath[] fieldPaths, EntityState state) {
        this.state = state;
        for (FieldPath fp : fieldPaths) {
            int idx = Collections.binarySearch(properties, fp);
            if (idx < 0) {
                // we can assume the field path to not be found only for Source 2
                Field field = ((S2DTClass) dtClass).getFieldForFieldPath(fp.s2());
                if (!field.isHiddenFieldPath()) {
                    log.warn("property at fieldpath {} for entity {} not found for update", fp, getName());
                }
                continue;
            }
            ObservableEntityProperty property = properties.get(idx);
            property.valueProperty().invalidate();
        }
    }

    public void performCountChanged(EntityState state) {
        this.state = state;
        beginChange();
        new CountUpdater(state.fieldPathIterator()).updateCount();
        endChange();
    }

    private class CountUpdater {

        private Iterator<FieldPath> leftIter;
        private int rightIdx = -1;
        private FieldPath left = null;
        private FieldPath right = null;
        private List<ObservableEntityProperty> akku = new ArrayList<>();

        public CountUpdater(Iterator<FieldPath> leftIter) {
            this.leftIter = leftIter;
        }

        private void advanceLeft() {
            left = leftIter.hasNext() ? leftIter.next() : null;
        }

        private void advanceRight() {
            if (rightIdx <= properties.size()) {
                rightIdx++;
                if (rightIdx < properties.size()) {
                    right = properties.get(rightIdx).getFieldPath();
                } else {
                    right = null;
                }
            } else {
                throw new UnsupportedOperationException("should never happen");
            }
        }

        private boolean rightHigher() {
            return left != null && (right == null || right.compareTo(left) > 0);
        }

        private boolean leftHigher() {
            return right != null && (left == null || left.compareTo(right) > 0);
        }

        private void invalidateAkkuPropertyBindings() {
            if (propertyBindings == null) return;
            for (ObservableEntityProperty p : akku) {
                int idx = Collections.binarySearch(propertyBindings, p.getFieldPath());
                if (idx < 0) continue;
                propertyBindings.get(idx).invalidate();
            }
        }

        private void updateCount() {
            advanceLeft();
            advanceRight();
            while (true) {
                if (Objects.equals(left, right)) {
                    if (left == null) break;
                    advanceLeft();
                    advanceRight();
                } else if (rightHigher()) {
                    do {
                        akku.add(createProperty(left));
                        advanceLeft();
                    } while (rightHigher());
                    int n = akku.size();
                    properties.addAll(rightIdx, akku);
                    nextAdd(rightIdx, rightIdx + n);
                    rightIdx += n;
                    invalidateAkkuPropertyBindings();
                    akku.clear();
                } else if (leftHigher()) {
                    int baseIdx = rightIdx;
                    do {
                        akku.add(properties.get(rightIdx));
                        advanceRight();
                    } while (leftHigher());
                    int n = akku.size();
                    properties.removeAll(akku);
                    nextRemove(baseIdx, akku);
                    rightIdx -= n;
                    invalidateAkkuPropertyBindings();
                    akku.clear();
                } else {
                    throw new UnsupportedOperationException("should never happen");
                }
            }
        }
    }

    public DTClass getDtClass() {
        return dtClass;
    }

    @Override
    public ObservableEntityProperty get(int index) {
        return properties.get(index);
    }

    @Override
    public int size() {
        return properties != null ? properties.size() : 0;
    }

    public int getIndex() {
        return index.get();
    }

    public ReadOnlyIntegerProperty indexProperty() {
        return index;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public class ObservableEntityPropertyBinding extends ObjectBinding<ObservableEntityProperty> implements Comparable<FieldPath> {
        private final FieldPath fp;
        private ObservableEntityPropertyBinding(FieldPath fp) {
            this.fp = fp;
        }
        @Override
        protected ObservableEntityProperty computeValue() {
            int idx = Collections.binarySearch(properties, fp);
            if (idx < 0) return null;
            return properties.get(idx);
        }
        @Override
        public int compareTo(FieldPath o) {
            return fp.compareTo(o);
        }
    }

    public ObservableEntityPropertyBinding getPropertyBinding(FieldPath fp) {
        if (propertyBindings == null) {
            propertyBindings = new ArrayList<>();
        }
        int idx = Collections.binarySearch(propertyBindings, fp);
        if (idx >= 0) return propertyBindings.get(idx);
        ObservableEntityPropertyBinding binding = new ObservableEntityPropertyBinding(fp);
        propertyBindings.add(-idx - 1, binding);
        return binding;
    }

    public <T> ObjectBinding<T> getPropertyBinding(Class<T> propertyClass, String name, T defaultValue) {
        ObjectBinding<T> defaultBinding = new ObjectBinding<T>() {
            @Override
            protected T computeValue() {
                return defaultValue;
            }
        };
        FieldPath fp = getDtClass().getFieldPathForName(name);
        if (fp == null) return defaultBinding;
        ObservableEntityPropertyBinding propertyBinding = getPropertyBinding(fp);
        return (ObjectBinding<T>) EasyBind.select(propertyBinding)
                .selectObject(ObservableEntityProperty::valueProperty)
                .map(propertyClass::cast)
                .orElse(defaultBinding);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getName(), getIndex());
    }

}
