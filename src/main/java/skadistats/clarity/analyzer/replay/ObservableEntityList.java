package skadistats.clarity.analyzer.replay;

import javafx.collections.ObservableListBase;
import skadistats.clarity.analyzer.util.PendingActionList;
import skadistats.clarity.event.Insert;
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
import skadistats.clarity.processor.runner.Context;

public class ObservableEntityList extends ObservableListBase<ObservableEntity> {

    @Insert
    private Context ctx;
    private final EngineType engineType;
    private final PendingActionList pendingActions = new PendingActionList("pendingActions");
    private ObservableEntity[] entities;

    public ObservableEntityList(EngineType engineType) {
        this.engineType = engineType;
        entities = new ObservableEntity[1 << engineType.getIndexBits()];
        for (var i = 0; i < entities.length; i++) {
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

    public ObservableEntity byHandle(Integer handle) {
        if (handle != null) {
            var idx = engineType.indexForHandle(handle);
            var serial = engineType.serialForHandle(handle);
            var entityAtIdx = get(idx);
            if (entityAtIdx != null) {
                if (entityAtIdx.getSerial() == serial) {
                    return entityAtIdx;
                }
            }
        }
        return null;
    }

    private void replaceEntity(int tick, int idx, ObservableEntity value) {
        entities[idx] = value;
        entities[idx].performCreate(ctx.getTick());
        nextUpdate(idx);
    }

    @OnEntityCreated
    protected void onCreate(Entity entity) {
        var i = entity.getIndex();
        var dtClass = entity.getDtClass();
        var serial = entity.getSerial();
        var state = entity.getState().copy();
        pendingActions.add(() -> replaceEntity(ctx.getTick(), i, new ObservableEntity(i, serial, dtClass, state)));
    }

    @OnEntityUpdated
    protected void onUpdate(Entity entity, FieldPath[] fieldPaths, int num) {
        var i = entity.getIndex();
        var fieldPathsCopy = new FieldPath[num];
        System.arraycopy(fieldPaths, 0, fieldPathsCopy, 0, num);
        var state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performUpdate(ctx.getTick(), fieldPathsCopy, state));
    }

    @OnEntityPropertyCountChanged
    protected void onPropertyCountChange(Entity entity) {
        var i = entity.getIndex();
        var state = entity.getState().copy();
        pendingActions.add(() -> entities[i].performCountChanged(state));
    }

    @OnEntityDeleted
    protected void onDelete(Entity entity) {
        var i = entity.getIndex();
        pendingActions.add(() -> replaceEntity(ctx.getTick(), i, new ObservableEntity(i)));
    }

    @OnEntityUpdatesCompleted
    protected void onUpdatesCompleted() {
        pendingActions.add(() -> {
            for (var i = 0; i < entities.length; i++) {
                if (entities[i].getDtClass() != null) {
                    entities[i].updatesFinished(ctx.getTick());
                }
            }
        });
        pendingActions.schedule(this::beginChange, this::endChange);
    }

}
