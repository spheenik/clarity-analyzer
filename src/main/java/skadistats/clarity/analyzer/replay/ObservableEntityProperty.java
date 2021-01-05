package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import skadistats.clarity.model.FieldPath;

import java.util.function.Supplier;

public class ObservableEntityProperty implements Comparable<FieldPath> {

    private final ReadOnlyObjectProperty<FieldPath> fieldPath;
    private final ReadOnlyStringWrapper type;
    private final ReadOnlyStringProperty name;
    private final ObjectBinding value;
    private long lastChangedAt;

    public ObservableEntityProperty(FieldPath fp, String type, String name, Supplier<Object> valueSupplier) {
        this.fieldPath = new ReadOnlyObjectWrapper<>(fp);
        this.type = new ReadOnlyStringWrapper(type);
        this.name = new ReadOnlyStringWrapper(name);
        this.value = new ObjectBinding() {
            @Override
            protected Object computeValue() {
                return valueSupplier.get();
            }
            @Override
            protected void onInvalidating() {
                lastChangedAt = System.currentTimeMillis();
            }
        };
    }

    public FieldPath getFieldPath() {
        return fieldPath.get();
    }

    public ReadOnlyObjectProperty<FieldPath> fieldPathProperty() {
        return fieldPath;
    }

    public String getType() {
        return type.get();
    }

    public ReadOnlyStringWrapper typeProperty() {
        return type;
    }

    public String getName() {
        return name.get();
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public Object getValue() {
        return value.get();
    }

    public ObjectBinding valueProperty() {
        return value;
    }

    public long getLastChangedAt() {
        return lastChangedAt;
    }

    @Override
    public int compareTo(FieldPath o) {
        return getFieldPath().compareTo(o);
    }

    @Override
    public String toString() {
        return String.format("%s %s", getFieldPath(), name.get());
    }

}
