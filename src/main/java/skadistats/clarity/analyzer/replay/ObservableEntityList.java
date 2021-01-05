package skadistats.clarity.analyzer.replay;

import javafx.collections.ObservableListBase;
import skadistats.clarity.analyzer.util.PendingActionList;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityPropertyCountChanged;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.OnEntityUpdatesCompleted;

public class ObservableEntityList extends ObservableListBase<ObservableEntity> {

    private final PendingActionList pendingActions = new PendingActionList("pendingActions");
    private ObservableEntity[] entities;

    public ObservableEntityList(EngineType engineType) {
        entities = new ObservableEntity[1 << engineType.getIndexBits()];
        for (int i = 0; i < entities.length; i++) {
            entities[i] = new ObservableEntity(i);
        }
    }

    @Override
    public ObservableEntity get(int index) {
        return entities[index];
    }

    @Override
    public int size() {
        return entities.length;
    }

    private void performUpdate(int i, ObservableEntity value) {
        entities[i] = value;
        nextUpdate(i);
    }

    @OnEntityCreated
    public void onCreate(Entity entity) {
        int i = entity.getIndex();
        DTClass dtClass = entity.getDtClass();
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> performUpdate(i, new ObservableEntity(i, dtClass, state)));
    }

    @OnEntityUpdated
    public void onUpdate(Entity entity, FieldPath[] fieldPaths, int num) {
        int i = entity.getIndex();
        FieldPath[] fieldPathsCopy = new FieldPath[num];
        System.arraycopy(fieldPaths, 0, fieldPathsCopy, 0, num);
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performUpdate(fieldPathsCopy, state));
    }

    @OnEntityPropertyCountChanged
    public void onPropertyCountChange(Entity entity) {
        int i = entity.getIndex();
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performCountChanged(state));
    }

    @OnEntityDeleted
    public void onDelete(Entity entity) {
        int i = entity.getIndex();
        pendingActions.add(() -> performUpdate(i, new ObservableEntity(i)));
    }

    @OnEntityUpdatesCompleted
    public void onUpdatesCompleted() {
        pendingActions.schedule(this::beginChange, this::endChange);
    }

}
