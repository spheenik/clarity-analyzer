package skadistats.clarity.analyzer.replay;

import com.tobiasdiez.easybind.EasyBind;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.ObservableListBase;
import lombok.extern.slf4j.Slf4j;
import skadistats.clarity.analyzer.util.TimeToTick;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.S2DTClass;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.util.StateDifferenceEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
public class ObservableEntity extends ObservableListBase<ObservableEntityProperty> {

    private final DTClass dtClass;
    private final ReadOnlyIntegerProperty index;
    private final ReadOnlyIntegerWrapper serial;
    private final ReadOnlyStringProperty name;
    private final List<ObservableEntityProperty> properties;
    private List<ObservableEntityPropertyBinding> propertyBindings;
    private final ObjectSet<FieldPath> recentChanges;
    private final SimpleIntegerProperty recentChangesHash;

    private EntityState state;

    public ObservableEntity(int index) {
        this(index, 0, null, null);
    }
    public ObservableEntity(int index, int serial, DTClass dtClass, EntityState state) {
        this.dtClass = dtClass;
        this.index = new ReadOnlyIntegerWrapper(index);
        this.serial = new ReadOnlyIntegerWrapper(serial);
        this.state = state;
        this.propertyBindings = null;
        if (dtClass != null) {
            this.name = new ReadOnlyStringWrapper(dtClass.getDtName());
            this.properties = createProperties(state);
        } else {
            this.name = new ReadOnlyStringWrapper("");
            this.properties = null;
        }
        this.recentChanges = new ObjectOpenHashSet<>();
        this.recentChangesHash = new SimpleIntegerProperty();
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

    void performUpdate(int tick, FieldPath[] fieldPaths, EntityState state) {
        this.state = state;
        for (FieldPath fp : fieldPaths) {
            int idx = Collections.binarySearch(properties, fp);
            if (idx < 0) {
                // we can assume the field path to not be found only for Source 2
                Field field = ((S2DTClass) dtClass).getFieldForFieldPath(fp.s2());
                if (!field.isHiddenFieldPath()) {
                    log.warn("property at fieldpath {} for entity {} ({}) not found for update", fp, getName(), getIndex());
                }
                continue;
            }
            ObservableEntityProperty property = properties.get(idx);
            property.valueProperty().invalidate();
            recentChanges.add(fp);
        }
    }

    void updatesFinished(int tick) {
        ObjectIterator<FieldPath> changeIterator = recentChanges.iterator();
        while (changeIterator.hasNext()) {
            FieldPath fp = changeIterator.next();
            int idx = Collections.binarySearch(properties, fp);
            if (idx < 0 || TimeToTick.millisBetweenTicks(tick, properties.get(idx).getLastChangedAtTick()) > 10000.0f) {
                changeIterator.remove();
            }
        }
        int oldHash = recentChangesHash.get();
        int newHash = recentChanges.hashCode();
        if (oldHash != newHash) {
            recentChangesHash.set(newHash);
        }
    }

    public void performCountChanged(EntityState state) {
        EntityState oldState = this.state;
        this.state = state;

        beginChange();
        new StateDifferenceEvaluator(oldState, state) {
            @Override
            protected void onPropertiesDeleted(List<FieldPath> fieldPaths) {
                int idx = Collections.binarySearch(properties, fieldPaths.get(0));
                List<ObservableEntityProperty> subList = properties.subList(idx, idx + fieldPaths.size());
                List<ObservableEntityProperty> removed = new ArrayList<>(subList);
                subList.clear();
                for (ObservableEntityProperty property : removed) {
                    recentChanges.remove(property.getFieldPath());
                }
                nextRemove(idx, removed);
            }
            @Override
            protected void onPropertiesAdded(List<FieldPath> fieldPaths) {
                int idx = -Collections.binarySearch(properties, fieldPaths.get(0)) - 1;
                int size = fieldPaths.size();
                List<ObservableEntityProperty> added = new ArrayList<>(size);
                for (FieldPath fieldPath : fieldPaths) {
                    added.add(createProperty(fieldPath));
                    recentChanges.add(fieldPath);
                }
                properties.addAll(idx, added);
                nextAdd(idx, idx + size);
            }
            @Override
            protected void onPropertyChanged(FieldPath fieldPath) {
            }
        }.work();
        endChange();
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

    public int getSerial() {
        return serial.get();
    }

    public ReadOnlyIntegerWrapper serialProperty() {
        return serial;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public int getRecentChangesHash() {
        return recentChangesHash.get();
    }

    public ReadOnlyIntegerProperty recentChangesHashProperty() {
        return recentChangesHash;
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

    public <T> ObservableValue<T> getPropertyBinding(Class<T> propertyClass, String name, T defaultValue) {
        FieldPath fp = getDtClass().getFieldPathForName(name);
        if (fp == null) {
            return new ObservableValueBase<>() {
                @Override
                public T getValue() {
                    return defaultValue;
                }
            };
        }
        ObservableEntityPropertyBinding propertyBinding = getPropertyBinding(fp);
        return EasyBind.select(propertyBinding)
                .selectObject(ObservableEntityProperty::valueProperty)
                .map(propertyClass::cast)
                .orElse(defaultValue);
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getName(), getIndex());
    }

}
