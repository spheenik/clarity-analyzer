package skadistats.clarity.analyzer.replay;

import javafx.application.Platform;
import javafx.collections.ObservableListBase;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.common.proto.Demo;

import java.util.concurrent.locks.ReentrantLock;

public class ObservableEntityList extends ObservableListBase<ObservableEntity> {

    private ReentrantLock lock = new ReentrantLock();
    private ObservableEntity[] entities;
    private boolean changeActive = false;

    public ObservableEntityList(EngineType engineType) {
        entities = new ObservableEntity[1 << engineType.getIndexBits()];
    }

    @Override
    public ObservableEntity get(int index) {
        return entities[index];
    }

    @Override
    public int size() {
        return entities.length;
    }

    private void ensureChangeOpen() {
        if (!changeActive) {
            changeActive = true;
            beginChange();
            Platform.runLater(this::commitChange);
        }
    }

    private void commitChange() {
        lock.lock();
        try {
            endChange();
            changeActive = false;
            for (int i = 0; i < entities.length; i++) {
                if (entities[i] != null) {
                    entities[i].commitChange();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @OnReset
    public void onReset(Context ctx, Demo.CDemoStringTables packet, ResetPhase phase) {
        lock.lock();
        try {
            if (phase == ResetPhase.CLEAR) {
                ensureChangeOpen();
                for (int i = 0; i < entities.length; i++) {
                    if (entities[i] != null) {
                        nextSet(i, entities[i]);
                        entities[i] = null;
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @OnEntityCreated
    public void onCreate(Context ctx, Entity entity) {
        lock.lock();
        try {
            ensureChangeOpen();
            int i = entity.getIndex();
            nextSet(i, entities[i]);
            entities[i] = new ObservableEntity(entity);
        } finally {
            lock.unlock();
        }
    }

    @OnEntityUpdated
    public void onUpdate(Context ctx, Entity entity, FieldPath[] fieldPaths, int num) {
        lock.lock();
        try {
            ensureChangeOpen();
            int i = entity.getIndex();
            entities[i].update(fieldPaths, num);
        } finally {
            lock.unlock();
        }
    }

    @OnEntityDeleted
    public void onDelete(Context ctx, Entity entity) {
        lock.lock();
        try {
            ensureChangeOpen();
            int i = entity.getIndex();
            nextSet(i, entities[i]);
            entities[i] = null;
        } finally {
            lock.unlock();
        }
    }

}
