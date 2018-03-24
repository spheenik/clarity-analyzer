package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

public class ObservableEntityProperty {

    private final StringBinding index;
    private final StringBinding name;
    private final StringBinding value;
    private final ObjectBinding raw;
    private boolean dirty = false;
    private long lastChangedAt;

    public ObservableEntityProperty(Entity entity, FieldPath fieldPath) {
        index = new StringBinding() {
            @Override
            protected String computeValue() {
                return fieldPath.toString();
            }
        };
        name = new StringBinding() {
            @Override
            protected String computeValue() {
                return entity.getDtClass().getNameForFieldPath(fieldPath);
            }
        };
        value = new StringBinding() {
            @Override
            protected String computeValue() {
                Object value = entity.getPropertyForFieldPath(fieldPath);
                return value != null ? value.toString() : "-";
            }
        };
        raw = new ObjectBinding() {
            @Override
            protected Object computeValue() {
                return entity.getPropertyForFieldPath(fieldPath);
            }
        };
    }

    public String getIndex() {
        return index.get();
    }

    public StringBinding indexProperty() {
        return index;
    }

    public String getName() {
        return name.get();
    }

    public StringBinding nameProperty() {
        return name;
    }

    public String getValue() {
        return value.get();
    }

    public StringBinding valueProperty() {
        return value;
    }

    public Object getRaw() {
        return raw.get();
    }

    public <T> ObjectBinding<T> rawProperty() {
        return raw;
    }

    public long getLastChangedAt() {
        return lastChangedAt;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (dirty) {
            lastChangedAt = System.currentTimeMillis();
        }
    }
}
