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

    private final EngineType engineType;
    private final PendingActionList pendingActions = new PendingActionList("pendingActions");
    private ObservableEntity[] entities;

    public ObservableEntityList(EngineType engineType) {
        this.engineType = engineType;
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

    public EngineType getEngineType() {
        return engineType;
    }

    private void performUpdate(int i, ObservableEntity value) {
        entities[i] = value;
        nextUpdate(i);
    }

    @OnEntityCreated
    protected void onCreate(Entity entity) {
        int i = entity.getIndex();
        DTClass dtClass = entity.getDtClass();
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> performUpdate(i, new ObservableEntity(i, dtClass, state)));
    }

    @OnEntityUpdated
    protected void onUpdate(Entity entity, FieldPath[] fieldPaths, int num) {
        int i = entity.getIndex();
        FieldPath[] fieldPathsCopy = new FieldPath[num];
        System.arraycopy(fieldPaths, 0, fieldPathsCopy, 0, num);
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performUpdate(fieldPathsCopy, state));
    }

    @OnEntityPropertyCountChanged
    protected void onPropertyCountChange(Entity entity) {
        int i = entity.getIndex();
        EntityState state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performCountChanged(state));
    }

    @OnEntityDeleted
    protected void onDelete(Entity entity) {
        int i = entity.getIndex();
        pendingActions.add(() -> performUpdate(i, new ObservableEntity(i)));
    }

    @OnEntityUpdatesCompleted
    protected void onUpdatesCompleted() {
        pendingActions.schedule(this::beginChange, this::endChange);
    }

}
