package skadistats.clarity.analyzer.map.dota;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal reader for single-file Valve VPK archives (v1/v2), as used by Dota's map packages.
 * Only entries stored inside the archive itself (archive index 0x7fff) can be read.
 */
public final class Vpk implements AutoCloseable {

    private static final int SIGNATURE = 0x55aa1234;
    private static final int EMBEDDED_ARCHIVE = 0x7fff;

    private record Entry(byte[] preload, int archiveIndex, long offset, int length) {}

    private final RandomAccessFile file;
    private final long dataStart;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public Vpk(Path path) throws IOException {
        file = new RandomAccessFile(path.toFile(), "r");
        var header = read(0, 28);
        if (header.getInt(0) != SIGNATURE) {
            throw new IOException(path + " is not a VPK archive");
        }
        var version = header.getInt(4);
        var treeSize = header.getInt(8);
        var headerSize = switch (version) {
            case 1 -> 12;
            case 2 -> 28;
            default -> throw new IOException("unsupported VPK version " + version + " in " + path);
        };
        dataStart = headerSize + (long) treeSize;
        readTree(read(headerSize, treeSize));
    }

    private void readTree(ByteBuffer tree) {
        while (true) {
            var ext = readString(tree);
            if (ext.isEmpty()) break;
            while (true) {
                var dir = readString(tree);
                if (dir.isEmpty()) break;
                while (true) {
                    var name = readString(tree);
                    if (name.isEmpty()) break;
                    tree.getInt();
                    var preloadBytes = Short.toUnsignedInt(tree.getShort());
                    var archiveIndex = Short.toUnsignedInt(tree.getShort());
                    var offset = Integer.toUnsignedLong(tree.getInt());
                    var length = tree.getInt();
                    tree.getShort();
                    var preload = new byte[preloadBytes];
                    tree.get(preload);
                    var fullName = (dir.equals(" ") ? "" : dir + "/") + name + (ext.equals(" ") ? "" : "." + ext);
                    entries.put(fullName, new Entry(preload, archiveIndex, offset, length));
                }
            }
        }
    }

    public Optional<String> findFirst(String prefix, String suffix) {
        return entries.keySet().stream().filter(n -> n.startsWith(prefix) && n.endsWith(suffix)).findFirst();
    }

    public byte[] readEntry(String name) throws IOException {
        var e = entries.get(name);
        if (e == null) {
            throw new IOException("no entry " + name);
        }
        if (e.length() > 0 && e.archiveIndex() != EMBEDDED_ARCHIVE) {
            throw new IOException("entry " + name + " lives in external archive " + e.archiveIndex());
        }
        var result = new byte[e.preload().length + e.length()];
        System.arraycopy(e.preload(), 0, result, 0, e.preload().length);
        if (e.length() > 0) {
            file.seek(dataStart + e.offset());
            file.readFully(result, e.preload().length, e.length());
        }
        return result;
    }

    private ByteBuffer read(long position, int length) throws IOException {
        var buf = new byte[length];
        file.seek(position);
        file.readFully(buf);
        return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static String readString(ByteBuffer b) {
        var start = b.position();
        var end = start;
        while (b.get(end) != 0) {
            end++;
        }
        b.position(end + 1);
        return new String(b.array(), start, end - start, StandardCharsets.UTF_8);
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

}
