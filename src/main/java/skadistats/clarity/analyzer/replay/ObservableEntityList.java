package skadistats.clarity.analyzer.replay;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.Handle;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.reader.OnReset;
import skadistats.clarity.processor.reader.ResetPhase;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.wire.proto.Demo;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;

@ApplicationScoped
public class ObservableEntityList {

    private final ObservableList<Entity> entities = FXCollections.observableList(new ArrayList<>(1 << Handle.INDEX_BITS));

    public void clear() {
        entities.clear();
    }

    @OnReset
    public void onReset(Context ctx, Demo.CDemoFullPacket packet, ResetPhase phase) {
        if (phase == ResetPhase.CLEAR) {
            clear();
        }
    }

    @OnEntityCreated
    public void onCreate(Context ctx, Entity entity) {
        entities.add(offsetForIndex(entity.getIndex()), entity);
    }

    @OnEntityDeleted
    public void onDelete(Context ctx, Entity entity) {
        entities.remove(entity);
    }

    public ObservableList<Entity> getEntities() {
        return entities;
    }

    public Callback<TableColumn.CellDataFeatures<Entity, Integer>, ObservableValue<Integer>> getIndexCellFactory() {
        return param -> new ReadOnlyIntegerWrapper(param.getValue().getIndex()).asObject();
    }

    public Callback<TableColumn.CellDataFeatures<Entity, String>, ObservableValue<String>> getDtClassCellFactory() {
        return param -> new ReadOnlyStringWrapper(param.getValue().getDtClass().getDtName());
    }

    private int offsetForIndex(int idx) {
        int a = -1; // lower bound
        int b = entities.size(); // upper bound
        while (a + 1 != b) {
            int  m = (a + b) >>> 1;
            if (entities.get(m).getIndex() < idx) {
                a = m;
            } else {
                b = m;
            }
        }
        return b;
    }
}
