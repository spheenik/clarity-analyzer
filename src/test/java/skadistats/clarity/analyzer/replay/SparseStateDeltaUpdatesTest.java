package skadistats.clarity.analyzer.replay;

import javafx.beans.value.ObservableValue;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityPropertyCountChanged;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.OnEntityUpdatesCompleted;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.state.EntityState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Drives {@link ObservableEntity} with the same create / sparse-delta update /
 * count-change sequence {@link ObservableEntityList} produces, synchronously
 * and without the FX toolkit, against a real replay. Every mirrored entity is
 * periodically compared with the live parser-side entity.
 */
public class SparseStateDeltaUpdatesTest {

    private static final String REPLAY = "dota/s2/normal/1648457986.dem";
    private static final int VERIFY_EVERY_TICKS = 3000;

    private Mirror mirror;

    @BeforeClass
    public void runReplay() throws Exception {
        var replay = Path.of(System.getProperty("clarity.replays", "replays"), REPLAY);
        if (!Files.isRegularFile(replay)) {
            throw new SkipException("replay not available: " + replay);
        }
        mirror = new Mirror();
        new SimpleRunner(new MappedFileSource(replay.toString())).runWith(mirror);
        mirror.verifyAll();
    }

    @Test
    public void persistentStateTracksLiveEntitiesAcrossUpdates() {
        assertTrue(mirror.verifications >= 10, "verifications: " + mirror.verifications);
        assertTrue(mirror.appliedUpdates > 100_000, "applied updates: " + mirror.appliedUpdates);
    }

    @Test
    public void deltaCoversExactlyTheChangedFields() {
        assertEquals(mirror.deltaSizeMismatches, 0);
        assertTrue(mirror.threeFieldUpdates > 0, "no update changed exactly three fields");
    }

    @Test
    public void propertyBindingFollowsMergedValue() {
        assertNotNull(mirror.healthBinding, "no hero found to bind m_iHealth");
        assertTrue(mirror.healthBindingChanges > 10, "binding changes: " + mirror.healthBindingChanges);
        assertEquals(mirror.healthBinding.getValue(), Integer.valueOf(mirror.trackedHero.getInt("m_iHealth")));
    }

    public static class Mirror {

        private final Map<Integer, ObservableEntity> mirrors = new HashMap<>();
        private Entities entities;
        private int lastVerifiedTick = -VERIFY_EVERY_TICKS;

        int verifications;
        long appliedUpdates;
        long deltaSizeMismatches;
        long threeFieldUpdates;

        Entity trackedHero;
        ObservableValue<Integer> healthBinding;
        int healthBindingChanges;

        @OnEntityCreated
        public void onCreate(Context ctx, Entity e) {
            var oe = new ObservableEntity(e.getIndex(), e.getSerial(), e.getDtClass(), e.getState().copy());
            mirrors.put(e.getIndex(), oe);
            if (healthBinding == null && e.getDtClass().getDtName().startsWith("CDOTA_Unit_Hero_")) {
                trackedHero = e;
                healthBinding = oe.getPropertyBinding(Integer.class, "m_iHealth", -1);
                healthBinding.addListener((obs, o, n) -> healthBindingChanges++);
            }
        }

        @OnEntityUpdated
        public void onUpdate(Context ctx, Entity e, FieldPath[] fieldPaths, int num) {
            var fieldPathsCopy = Arrays.copyOf(fieldPaths, num);
            var delta = EntityState.captureChanged(e.getState(), fieldPathsCopy, num);
            if (delta.fields().length != num) deltaSizeMismatches++;
            if (num == 3) threeFieldUpdates++;
            mirrors.get(e.getIndex()).performUpdate(ctx.getTick(), fieldPathsCopy, delta);
            appliedUpdates++;
        }

        @OnEntityPropertyCountChanged
        public void onPropertyCountChanged(Context ctx, Entity e) {
            mirrors.get(e.getIndex()).performCountChanged(e.getState().copy());
        }

        @OnEntityDeleted
        public void onDelete(Context ctx, Entity e) {
            mirrors.remove(e.getIndex());
            if (e == trackedHero) {
                trackedHero = null;
                healthBinding = null;
            }
        }

        @OnEntityUpdatesCompleted
        public void onUpdatesCompleted(Context ctx) {
            entities = ctx.getProcessor(Entities.class);
            if (ctx.getTick() - lastVerifiedTick >= VERIFY_EVERY_TICKS) {
                lastVerifiedTick = ctx.getTick();
                verifyAll();
            }
        }

        void verifyAll() {
            for (var entry : mirrors.entrySet()) {
                var live = entities.getByIndex(entry.getKey());
                assertNotNull(live, "mirrored entity " + entry.getKey() + " not live");
                verify(entry.getValue(), live);
            }
            verifications++;
        }

        private static void verify(ObservableEntity oe, Entity live) {
            var liveFps = new ArrayList<FieldPath>();
            live.getState().fieldPathIterator().forEachRemaining(liveFps::add);
            assertEquals(oe.size(), liveFps.size(), oe + ": property count");
            for (var i = 0; i < liveFps.size(); i++) {
                var property = oe.get(i);
                var fp = liveFps.get(i);
                assertEquals(property.getFieldPath(), fp, oe + ": field path at " + i);
                Object expected = EntityState.getValueForFieldPath(live.getState(), fp);
                var actual = property.valueProperty().get();
                if (!Objects.deepEquals(expected, actual)) {
                    fail(oe + " " + oe.getNameForFieldPath(fp) + ": expected " + expected + " but was " + actual);
                }
            }
        }
    }

}
