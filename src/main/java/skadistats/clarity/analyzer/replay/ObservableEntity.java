package skadistats.clarity.analyzer.replay;

import com.tobiasdiez.easybind.EasyBind;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyFloatProperty;
import javafx.beans.property.ReadOnlyFloatWrapper;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.ObservableListBase;
import lombok.extern.slf4j.Slf4j;
import skadistats.clarity.analyzer.util.TickHelper;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.model.s1.S1DTClass;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.S2DTClass;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.state.EntityState;
import skadistats.clarity.state.StateDelta;
import skadistats.clarity.state.s2.S2EntityState;
import skadistats.clarity.util.FieldPathUtil;
import skadistats.clarity.util.StateDifferenceEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static skadistats.clarity.analyzer.replay.ObservableEntityCellType.DEFAULT;
import static skadistats.clarity.analyzer.replay.ObservableEntityCellType.HANDLE;

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

    /**
     * FX-thread-owned persistent state. Seeded from a full parse-side
     * snapshot at creation, mutated in place on each {@link #performUpdate}
     * via {@link EntityState#applyFrom} of the incoming delta. Parse thread
     * never reads or writes this field.
     */
    private EntityState fxState;
    private List<Object> primitiveBindingRetainers;

    public ObservableEntity(int index) {
        this(index, 0, null, null);
    }
    public ObservableEntity(int index, int serial, DTClass dtClass, EntityState state) {
        this.dtClass = dtClass;
        this.index = new ReadOnlyIntegerWrapper(index);
        this.serial = new ReadOnlyIntegerWrapper(serial);
        this.fxState = state;
        this.propertyBindings = null;
        if (dtClass != null) {
            this.name = new ReadOnlyStringWrapper(dtClass.getDtName());
            this.properties = createProperties(state);
        } else {
            this.name = new ReadOnlyStringWrapper("");
            this.properties = null;
        }
        this.recentChanges = new ObjectOpenHashSet<>();
        this.recentChangesHash = new SimpleIntegerProperty(0);
    }

    private List<ObservableEntityProperty> createProperties(EntityState state) {
        List<ObservableEntityProperty> properties = new ArrayList<>();
        var iter = state.fieldPathIterator();
        while (iter.hasNext()) {
            var fp = iter.next();
            var property = createProperty(fp);
            properties.add(property);
        }
        return properties;
    }


    public String getNameForFieldPath(FieldPath fp) {
        return DTClass.getNameForFieldPath(dtClass, fxState, fp);
    }

    public FieldPath getFieldPathForName(String name) {
        return DTClass.getFieldPathForName(dtClass, fxState, name);
    }

    private ObservableEntityProperty createProperty(FieldPath fp) {
        var propName = getNameForFieldPath(fp);
        record TypeInfo(String typeName, ObservableEntityCellType cellType) {}
        var typeInfo = switch (dtClass) {
            case S1DTClass s1 -> {
                var propType = s1.getReceiveProps()[((S1FieldPath) fp).idx()].getSendProp().getType();
                yield new TypeInfo(
                        propType.toString(),
                        cellTypeForS1(propName, propType)
                );
            }
            case S2DTClass ignored -> {
                var s2state = (S2EntityState) fxState;
                var fieldType = s2state.getTypeForFieldPath((S2FieldPath) fp);
                yield new TypeInfo(
                        fieldType.toString(),
                        cellTypeForS2(propName, fieldType)
                );
            }
        };
        var property = new ObservableEntityProperty(
                fp,
                typeInfo.cellType,
                typeInfo.typeName,
                propName,
                () -> EntityState.getValueForFieldPath(ObservableEntity.this.fxState, fp)
        );
        return property;
    }

    private ObservableEntityCellType cellTypeForS1(String propName, PropType propType) {
        if (propName.startsWith("m_h")) {
            return HANDLE;
        }
        return DEFAULT;
    }

    private ObservableEntityCellType cellTypeForS2(String propName, FieldType fieldType) {
        if ("CHandle".equals(fieldType.getBaseType())) {
            return HANDLE;
        }
        return DEFAULT;
    }

    public void performCreate(int tick) {
        if (properties == null) return;
        for (var property : properties) {
            recentChanges.add(property.getFieldPath());
        }
        updateRecentChangesHash();
    }

    /**
     * FX-thread entry point for a per-tick update. Merges the supplied
     * sparse {@link StateDelta} into the persistent {@link #fxState} for
     * each changed {@link FieldPath}, then invalidates the corresponding
     * property bindings.
     */
    void performUpdate(int tick, FieldPath[] fieldPaths, StateDelta delta) {
        for (var fp : fieldPaths) {
            EntityState.applyFrom(fxState, delta, fp);
            var idx = Collections.binarySearch(properties, fp);
            if (idx < 0) {
                // we can assume the field path to not be found only for Source 2
                var field = ((S2EntityState) fxState).getFieldForFieldPath((S2FieldPath) fp);
                if (!field.isHiddenFieldPath()) {
                    log.warn("property at fieldpath {} for entity {} ({}) not found for update", fp, getName(), getIndex());
                }
                continue;
            }
            var property = properties.get(idx);
            property.markChanged();
            property.valueProperty().invalidate();
            recentChanges.add(fp);
        }
    }

    void updatesFinished(int tick) {
        var changeIterator = recentChanges.iterator();
        while (changeIterator.hasNext()) {
            var fp = changeIterator.next();
            var idx = Collections.binarySearch(properties, fp);
            if (idx < 0 || !TickHelper.isRecent(properties.get(idx).getLastChangedAtTick())) {
                changeIterator.remove();
            }
        }
        updateRecentChangesHash();
    }

    private void updateRecentChangesHash() {
        var oldHash = recentChangesHash.get();
        var newHash = recentChanges.isEmpty() ? 0 : recentChanges.hashCode();
        if (oldHash != newHash) {
            recentChangesHash.set(newHash);
        }
    }

    public void performCountChanged(EntityState state) {
        var oldState = this.fxState;
        this.fxState = state;

        beginChange();
        new StateDifferenceEvaluator(oldState, state) {
            @Override
            protected void onPropertiesDeleted(List<FieldPath> fieldPaths) {
                var idx = Collections.binarySearch(properties, fieldPaths.get(0));
                var subList = properties.subList(idx, idx + fieldPaths.size());
                List<ObservableEntityProperty> removed = new ArrayList<>(subList);
                subList.clear();
                for (var property : removed) {
                    recentChanges.remove(property.getFieldPath());
                }
                nextRemove(idx, removed);
            }
            @Override
            protected void onPropertiesAdded(List<FieldPath> fieldPaths) {
                var idx = -Collections.binarySearch(properties, fieldPaths.get(0)) - 1;
                var size = fieldPaths.size();
                List<ObservableEntityProperty> added = new ArrayList<>(size);
                for (var fieldPath : fieldPaths) {
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
            var idx = Collections.binarySearch(properties, fp);
            if (idx < 0) return null;
            return properties.get(idx);
        }
        @Override
        public int compareTo(FieldPath other) {
            return FieldPathUtil.compare(fp, other);
        }
    }


    public ObservableEntityPropertyBinding getPropertyBinding(FieldPath fp) {
        if (propertyBindings == null) {
            propertyBindings = new ArrayList<>();
        }
        var idx = Collections.binarySearch(propertyBindings, fp);
        if (idx >= 0) return propertyBindings.get(idx);
        var binding = new ObservableEntityPropertyBinding(fp);
        propertyBindings.add(-idx - 1, binding);
        return binding;
    }

    public <T> ObservableValue<T> getPropertyBinding(Class<T> propertyClass, String name, T defaultValue) {
        var fp = getFieldPathForName(name);
        if (fp == null) {
            return new ObservableValueBase<>() {
                @Override
                public T getValue() {
                    return defaultValue;
                }
            };
        }
        var propertyBinding = getPropertyBinding(fp);
        return EasyBind.select(propertyBinding)
                .selectObject(ObservableEntityProperty::valueProperty)
                .map(propertyClass::cast)
                .orElse(defaultValue);
    }

    private ObservableValue<Object> primitiveSource(FieldPath fp) {
        var propertyBinding = getPropertyBinding(fp);
        var source = EasyBind.select(propertyBinding).selectObject(ObservableEntityProperty::valueProperty);
        if (primitiveBindingRetainers == null) {
            primitiveBindingRetainers = new ArrayList<>();
        }
        primitiveBindingRetainers.add(source);
        return source;
    }

    /**
     * FX-thread-only primitive-typed read of {@code name} from {@link #fxState}.
     * The returned property is invalidated on the same path as the generic
     * {@link #getPropertyBinding(Class, String, Object)} accessor, but
     * {@link ReadOnlyIntegerProperty#get} reads an {@code int} directly from
     * clarity's primitive state accessors — no boxing.
     */
    public ReadOnlyIntegerProperty getIntPropertyBinding(String name, int defaultValue) {
        var fp = getFieldPathForName(name);
        if (fp == null) {
            return new ReadOnlyIntegerWrapper(defaultValue).getReadOnlyProperty();
        }
        var wrapper = new ReadOnlyIntegerWrapper(fxState != null ? EntityState.getInt(fxState, fp) : defaultValue);
        primitiveSource(fp).addListener((obs, o, n) -> wrapper.set(EntityState.getInt(fxState, fp)));
        return wrapper.getReadOnlyProperty();
    }

    /**
     * FX-thread-only primitive-typed {@code long} accessor. See
     * {@link #getIntPropertyBinding(String, int)}.
     */
    public ReadOnlyLongProperty getLongPropertyBinding(String name, long defaultValue) {
        var fp = getFieldPathForName(name);
        if (fp == null) {
            return new ReadOnlyLongWrapper(defaultValue).getReadOnlyProperty();
        }
        var wrapper = new ReadOnlyLongWrapper(fxState != null ? EntityState.getLong(fxState, fp) : defaultValue);
        primitiveSource(fp).addListener((obs, o, n) -> wrapper.set(EntityState.getLong(fxState, fp)));
        return wrapper.getReadOnlyProperty();
    }

    /**
     * FX-thread-only primitive-typed {@code float} accessor. See
     * {@link #getIntPropertyBinding(String, int)}.
     */
    public ReadOnlyFloatProperty getFloatPropertyBinding(String name, float defaultValue) {
        var fp = getFieldPathForName(name);
        if (fp == null) {
            return new ReadOnlyFloatWrapper(defaultValue).getReadOnlyProperty();
        }
        var wrapper = new ReadOnlyFloatWrapper(fxState != null ? EntityState.getFloat(fxState, fp) : defaultValue);
        primitiveSource(fp).addListener((obs, o, n) -> wrapper.set(EntityState.getFloat(fxState, fp)));
        return wrapper.getReadOnlyProperty();
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", getName(), getIndex());
    }

}
