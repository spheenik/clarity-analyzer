package skadistats.clarity.analyzer.map.dota;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * World tree positions for a Dota 2 replay, read from the {@code .trm} table in the map package
 * of the local Dota installation.
 *
 * <p>Tree {@code i} in the returned list corresponds to bit {@code i} of {@code m_bWorldTreeState}.
 * The client picks the map package from {@code CSVCMsg_ServerInfo.protocol}; {@link #mapFor(int)}
 * mirrors that table (taken from the client binary).
 */
public final class DotaTrees {

    public record Tree(int x, int y, int layer) {}

    private static final Logger log = LoggerFactory.getLogger(DotaTrees.class);
    private static final int TRM_MAGIC = 0x706d7274;
    private static final Map<String, List<Tree>> CACHE = new ConcurrentHashMap<>();

    private DotaTrees() {}

    public static String mapFor(int serverProtocol) {
        return switch (serverProtocol) {
            case 1202, 1203 -> "dota_685";
            case 1204 -> "dota_688";
            case 1205 -> "dota_706";
            case 1206 -> "dota_719";
            case 1207 -> "dota_722";
            case 1208 -> "dota_728";
            case 1209 -> "dota_732";
            case 1210 -> "dota_737";
            default -> "dota";
        };
    }

    public static List<Tree> forServerProtocol(int serverProtocol) {
        return CACHE.computeIfAbsent(mapFor(serverProtocol), DotaTrees::loadForMap);
    }

    private static List<Tree> loadForMap(String map) {
        var mapsDir = DotaInstall.findMapsDir();
        if (mapsDir.isEmpty()) {
            log.warn("no Dota 2 installation found (set DOTA2_DIR), trees will not be shown");
            return List.of();
        }
        var vpkPath = mapsDir.get().resolve(map + ".vpk");
        if (!Files.isRegularFile(vpkPath)) {
            log.warn("map package {} not found, trees will not be shown", vpkPath);
            return List.of();
        }
        try (var vpk = new Vpk(vpkPath)) {
            var trm = vpk.findFirst("maps/", ".trm");
            if (trm.isEmpty()) {
                log.warn("map package {} has no tree table, trees will not be shown", vpkPath);
                return List.of();
            }
            var trees = parseTrm(vpk.readEntry(trm.get()));
            log.info("loaded {} tree positions from {}", trees.size(), vpkPath);
            return trees;
        } catch (IOException | RuntimeException e) {
            log.warn("failed to read tree positions from {}", vpkPath, e);
            return List.of();
        }
    }

    static List<Tree> parseTrm(byte[] data) throws IOException {
        var b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (b.getInt(0) != TRM_MAGIC) {
            throw new IOException("not a tree table");
        }
        var count = b.getInt(16);
        var start = data.length - count * 12;
        if (count < 0 || start < b.getInt(8)) {
            throw new IOException("tree table size mismatch");
        }
        var trees = new Tree[count];
        for (var i = 0; i < count; i++) {
            var o = start + i * 12;
            trees[i] = new Tree(b.getInt(o), b.getInt(o + 4), b.getInt(o + 8));
        }
        return List.of(trees);
    }

}
