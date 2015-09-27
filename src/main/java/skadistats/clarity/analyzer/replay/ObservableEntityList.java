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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ObservableEntityList extends ObservableListBase<ObservableEntity> {

    private static final int ST_REQUEST      = 0x01;
    private static final int ST_RESET        = 0x02;

    private static final int ST_CHANGE_LIST  = 0x10;
    private static final int ST_CHANGE_PROP  = 0x20;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition platformUpToDate = lock.newCondition();
    private int status = 0;

    private ObservableEntity[] entities;

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

    private void commitToPlatform() {
        lock.lock();
        try {
            if ((status & ST_CHANGE_LIST) != 0) {
                endChange();
                status &= ~ST_CHANGE_LIST;
            }
            if ((status & ST_CHANGE_PROP) != 0) {
                for (int i = 0; i < entities.length; i++) {
                    if (entities[i] != null) {
                        entities[i].commitChange();
                    }
                }
                status &= ~ST_CHANGE_PROP;
            }
            status &= ~ST_REQUEST;
            platformUpToDate.signal();
        } finally {
            lock.unlock();
        }
    }

    private void markChange(int type) {
        if ((type & ST_CHANGE_LIST) != 0 && (status & ST_CHANGE_LIST) == 0) {
            status |= ST_CHANGE_LIST;
            beginChange();
        }
        if ((type & ST_CHANGE_PROP) != 0 && (status & ST_CHANGE_PROP) == 0) {
            status |= ST_CHANGE_PROP;
        }
        if ((status & (ST_RESET | ST_REQUEST)) == 0 && (status & (ST_CHANGE_LIST | ST_CHANGE_PROP)) != 0) {
            status |= ST_REQUEST;
            Platform.runLater(this::commitToPlatform);
        }
    }

    @OnReset
    public void onReset(Context ctx, Demo.CDemoStringTables packet, ResetPhase phase) {
        lock.lock();
        try {
            if (phase == ResetPhase.CLEAR) {
                if ((status & ST_REQUEST) != 0) {
                    platformUpToDate.awaitUninterruptibly();
                }
                status |= ST_RESET;
                markChange(ST_CHANGE_LIST);
                for (int i = 0; i < entities.length; i++) {
                    if (entities[i] != null) {
                        nextSet(i, entities[i]);
                        entities[i] = null;
                    }
                }
            } else if (phase == ResetPhase.COMPLETE) {
                status &= ~ST_RESET;
                markChange(0);
            }
        } finally {
            lock.unlock();
        }
    }

    @OnEntityCreated
    public void onCreate(Context ctx, Entity entity) {
        lock.lock();
        try {
            markChange(ST_CHANGE_LIST);
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
            markChange(ST_CHANGE_PROP);
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
            markChange(ST_CHANGE_LIST);
            int i = entity.getIndex();
            nextSet(i, entities[i]);
            entities[i] = null;
        } finally {
            lock.unlock();
        }
    }

}
