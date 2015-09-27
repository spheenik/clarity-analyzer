package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.StringBinding;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

public class ObservableEntityProperty {

    private final StringBinding index;
    private final StringBinding name;
    private final StringBinding value;
    private boolean dirty = false;

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

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
