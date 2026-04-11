package skadistats.clarity.analyzer.replay;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.function.Consumer;

public class NavigationController {

    private final ObservableList<Integer> handles = FXCollections.observableArrayList();
    private final IntegerProperty idx = new SimpleIntegerProperty(-1);

    private final BooleanBinding canNavigateBackward;
    private final BooleanBinding canNavigateForward;

    private ObservableEntityList entityList;
    private Consumer<ObservableEntity> onNavigate;

    public NavigationController() {
        this.canNavigateBackward = Bindings.createBooleanBinding(
                () -> idx.get() > 0,
                idx
        );
        this.canNavigateForward = Bindings.createBooleanBinding(
                () -> idx.get() + 1 < handles.size(),
                handles, idx
        );
    }

    public void setEntityList(ObservableEntityList entityList) {
        this.entityList = entityList;
        clear();
    }

    public void setOnNavigate(Consumer<ObservableEntity> onNavigate) {
        this.onNavigate = onNavigate;
    }

    public void navigateTo(int handle) {
        // truncate forward history
        if (idx.get() + 1 < handles.size()) {
            handles.subList(idx.get() + 1, handles.size()).clear();
        }
        handles.add(handle);
        idx.set(handles.size() - 1);
        selectCurrent();
    }

    public void recordSelection(ObservableEntity entity) {
        if (entity == null || entity.getDtClass() == null || entityList == null) return;
        var handle = entityList.getEngineType().handleForIndexAndSerial(
                entity.getIndex(), entity.getSerial()
        );
        // truncate forward history
        if (idx.get() + 1 < handles.size()) {
            handles.subList(idx.get() + 1, handles.size()).clear();
        }
        // avoid duplicates
        if (handles.isEmpty() || !handles.get(handles.size() - 1).equals(handle)) {
            handles.add(handle);
            idx.set(handles.size() - 1);
        }
    }

    public void navigateBackward() {
        for (int i = idx.get() - 1; i >= 0; i--) {
            var entity = resolve(handles.get(i));
            if (entity != null) {
                idx.set(i);
                doNavigate(entity);
                return;
            }
        }
    }

    public void navigateForward() {
        for (int i = idx.get() + 1; i < handles.size(); i++) {
            var entity = resolve(handles.get(i));
            if (entity != null) {
                idx.set(i);
                doNavigate(entity);
                return;
            }
        }
    }

    public void clear() {
        handles.clear();
        idx.set(-1);
    }

    public Boolean getCanNavigateBackward() {
        return canNavigateBackward.get();
    }

    public BooleanBinding canNavigateBackwardProperty() {
        return canNavigateBackward;
    }

    public Boolean getCanNavigateForward() {
        return canNavigateForward.get();
    }

    public BooleanBinding canNavigateForwardProperty() {
        return canNavigateForward;
    }

    private void selectCurrent() {
        var entity = resolve(handles.get(idx.get()));
        if (entity != null) {
            doNavigate(entity);
        }
    }

    private void doNavigate(ObservableEntity entity) {
        if (onNavigate != null) {
            onNavigate.accept(entity);
        }
    }

    private ObservableEntity resolve(int handle) {
        if (entityList == null) return null;
        return entityList.byHandle(handle);
    }

}
