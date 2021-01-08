package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import skadistats.clarity.model.FieldPath;

import java.util.Arrays;
import java.util.function.Supplier;

import static javafx.beans.binding.Bindings.createStringBinding;

public class ObservableEntityProperty implements Comparable<FieldPath> {

    private final ReadOnlyObjectProperty<FieldPath> fieldPath;
    private final ReadOnlyStringWrapper type;
    private final ReadOnlyStringProperty name;
    private final ObjectBinding value;
    private final StringBinding valueAsString;
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
        this.valueAsString = createStringBinding(
                () -> {
                    Object v = value.get();
                    if (v == null) {
                        return "<NULL>";
                    } else if (v instanceof Object[]) {
                        return Arrays.toString((Object[]) v);
                    } else {
                        return v.toString();
                    }
                },
                this.value
        );
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

    public String getValueAsString() {
        return valueAsString.get();
    }

    public StringBinding valueAsStringProperty() {
        return valueAsString;
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
