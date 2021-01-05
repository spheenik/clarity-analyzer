package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import skadistats.clarity.model.FieldPath;

import java.util.function.Supplier;

public class ObservableEntityProperty implements Comparable<FieldPath> {

    final FieldPath fp;
    private final ReadOnlyStringProperty index;
    private final ReadOnlyStringProperty name;
    private final ObjectBinding value;
    private long lastChangedAt;

    public ObservableEntityProperty(FieldPath fp, String name, Supplier<Object> valueSupplier) {
        this.fp = fp;
        this.index = new ReadOnlyStringWrapper(fp.toString());
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

    public ReadOnlyStringProperty indexProperty() {
        return index;
    }

    public ReadOnlyStringProperty nameProperty() {
        return name;
    }

    public ObjectBinding valueProperty() {
        return value;
    }

    public long getLastChangedAt() {
        return lastChangedAt;
    }

    @Override
    public int compareTo(FieldPath o) {
        return fp.compareTo(o);
    }

    @Override
    public String toString() {
        return String.format("%s %s", index.get(), name.get());
    }

}
