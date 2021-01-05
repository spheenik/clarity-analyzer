package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableListBase;
import lombok.extern.slf4j.Slf4j;
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

    private EntityState state;

    public ObservableEntity(int index) {
        this(index, null, null);
    }

    public ObservableEntity(int index, DTClass dtClass, EntityState state) {
        this.dtClass = dtClass;
        this.index = new ReadOnlyIntegerWrapper(index);
        if (dtClass != null) {
            this.name = new ReadOnlyStringWrapper(dtClass.getDtName());
            this.properties = createProperties(state);
        } else {
            this.name = new ReadOnlyStringWrapper("");
            this.properties = null;
        }
        this.state = state;
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
        ObservableEntityProperty property = new ObservableEntityProperty(
                fp,
                dtClass.getNameForFieldPath(fp),
                () -> ObservableEntity.this.state.getValueForFieldPath(fp)
        );
        return property;
    }

    void performUpdate(FieldPath[] fieldPaths, EntityState state) {
        this.state = state;
        for (FieldPath fp : fieldPaths) {
            int idx = getIndexForFieldPath(fp);
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

    public int getIndexForFieldPath(FieldPath fp) {
        return Collections.binarySearch(properties, fp);
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
                    right = properties.get(rightIdx).fp;
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


    public <T> ObjectBinding<T> getPropertyBinding(Class<T> propertyClass, String name, T defaultValue) {
        FieldPath fp = getDtClass().getFieldPathForName(name);
        Integer idx = null;
        if (fp != null) {
            idx = getIndexForFieldPath(fp);
            if (idx < 0) {
                log.warn("property at fieldpath {} not found for property binding", fp);
            }
        }
        if (idx == null) {
            return Bindings.createObjectBinding(() -> defaultValue);
        } else {
            return properties.get(idx).valueProperty();
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getName(), getIndex());
    }

}
