package skadistats.clarity.analyzer.map.dota;

import io.airlift.compress.lz4.Lz4Decompressor;
import io.airlift.compress.zstd.ZstdDecompressor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Reader for binary KeyValues3 (versions 2 to 5), as found in the DATA block of compiled Source 2 resources.
 * Uncompressed, LZ4 and zstd data is supported, except LZ4-compressed binary blobs.
 *
 * <p>Objects are returned as {@link LinkedHashMap}, arrays as {@link List}, binary blobs as {@code byte[]}.
 * Format as implemented by ValveResourceFormat (MIT).
 */
public final class Kv3 {

    private static final int MAGIC = 0x4B563300;
    private static final int TRAILER = 0xFFEEDD00;
    private static final int COMPRESSION_NONE = 0;
    private static final int COMPRESSION_LZ4 = 1;
    private static final int COMPRESSION_ZSTD = 2;

    private static final int NULL = 1, BOOLEAN = 2, INT64 = 3, UINT64 = 4, DOUBLE = 5, STRING = 6, BINARY_BLOB = 7,
            ARRAY = 8, OBJECT = 9, ARRAY_TYPED = 10, INT32 = 11, UINT32 = 12, BOOLEAN_TRUE = 13, BOOLEAN_FALSE = 14,
            INT64_ZERO = 15, INT64_ONE = 16, DOUBLE_ZERO = 17, DOUBLE_ONE = 18, FLOAT = 19, INT16 = 20, UINT16 = 21,
            INT32_AS_BYTE = 23, ARRAY_TYPE_BYTE_LENGTH = 24, ARRAY_TYPE_AUXILIARY_BUFFER = 25;

    private static final class Buffers {
        ByteBuffer b1, b2, b4, b8;
    }

    private final int version;
    private String[] strings;
    private ByteBuffer types;
    private ByteBuffer objectLengths;
    private ByteBuffer blobLengths;
    private ByteBuffer blobs;
    private Buffers buffer;
    private Buffers auxiliary;

    private Kv3(int version) {
        this.version = version;
    }

    /** Parses the DATA block of a compiled Source 2 resource file (e.g. {@code .vents_c}). */
    public static Object readResourceData(byte[] resource) throws IOException {
        var b = le(ByteBuffer.wrap(resource));
        var blockOffset = b.getInt(8);
        var blockCount = b.getInt(12);
        for (var i = 0; i < blockCount; i++) {
            var entry = 8 + blockOffset + i * 12;
            if (b.getInt(entry) == 0x41544144) {
                var start = entry + 4 + b.getInt(entry + 4);
                return read(le(b.slice(start, b.getInt(entry + 8))));
            }
        }
        throw new IOException("resource has no DATA block");
    }

    public static Object read(ByteBuffer in) throws IOException {
        in = le(in);
        var magic = in.getInt();
        if ((magic & 0xFFFFFF00) != MAGIC) {
            throw new IOException(String.format("not binary KV3 (magic %08x)", magic));
        }
        var version = magic & 0xFF;
        if (version < 2 || version > 5) {
            throw new IOException("unsupported KV3 version " + version);
        }
        return new Kv3(version).parse(in);
    }

    private Object parse(ByteBuffer in) throws IOException {
        in.position(in.position() + 16);
        var compression = in.getInt();
        in.getShort();
        in.getShort();
        var countBytes1 = in.getInt();
        var countBytes4 = in.getInt();
        var countBytes8 = in.getInt();
        var countTypes = in.getInt();
        in.getShort();
        in.getShort();
        var sizeUncompressedTotal = in.getInt();
        var sizeCompressedTotal = in.getInt();
        var countBlocks = in.getInt();
        var sizeBinaryBlobs = in.getInt();
        var countBytes2 = 0;
        if (version >= 4) {
            countBytes2 = in.getInt();
            in.getInt();
        }
        int sizeUncompressed1, sizeCompressed1, sizeUncompressed2 = 0, sizeCompressed2 = 0;
        int countBytes1b2 = 0, countBytes2b2 = 0, countBytes4b2 = 0, countBytes8b2 = 0, countObjectsB2 = 0;
        if (version >= 5) {
            sizeUncompressed1 = in.getInt();
            sizeCompressed1 = in.getInt();
            sizeUncompressed2 = in.getInt();
            sizeCompressed2 = in.getInt();
            countBytes1b2 = in.getInt();
            countBytes2b2 = in.getInt();
            countBytes4b2 = in.getInt();
            countBytes8b2 = in.getInt();
            in.getInt();
            countObjectsB2 = in.getInt();
            in.getInt();
            in.getInt();
        } else {
            sizeUncompressed1 = sizeUncompressedTotal;
            sizeCompressed1 = sizeCompressedTotal;
        }
        if (compression != COMPRESSION_NONE && compression != COMPRESSION_LZ4 && compression != COMPRESSION_ZSTD) {
            throw new IOException("unsupported KV3 compression " + compression);
        }

        var extra = version < 5 && compression == COMPRESSION_ZSTD ? sizeBinaryBlobs : 0;
        var buffer1 = le(ByteBuffer.wrap(decompress(in, compression, sizeCompressed1, sizeUncompressed1 + extra)));
        var buffer1Main = le(buffer1.slice(0, sizeUncompressed1));
        ByteBuffer blobSizes = null;

        var b1 = new Buffers();
        var offset = 0;
        if (countBytes1 > 0) {
            b1.b1 = slice(buffer1Main, offset, countBytes1);
            offset += countBytes1;
        }
        if (countBytes2 > 0) {
            offset = align(offset, 2);
            b1.b2 = slice(buffer1Main, offset, countBytes2 * 2);
            offset += countBytes2 * 2;
        }
        if (countBytes4 > 0) {
            offset = align(offset, 4);
            b1.b4 = slice(buffer1Main, offset, countBytes4 * 4);
            offset += countBytes4 * 4;
        }
        if (countBytes8 > 0) {
            offset = align(offset, 8);
            b1.b8 = slice(buffer1Main, offset, countBytes8 * 8);
            offset += countBytes8 * 8;
        } else if (version < 5) {
            offset = align(offset, 8);
        }

        strings = new String[b1.b4.getInt()];
        if (version >= 5) {
            auxiliary = b1;
            for (var i = 0; i < strings.length; i++) {
                strings[i] = readCString(b1.b1);
            }
        } else {
            buffer = b1;
            var stringsStart = offset;
            var stringData = slice(buffer1Main, offset, sizeUncompressed1 - offset);
            for (var i = 0; i < strings.length; i++) {
                strings[i] = readCString(stringData);
            }
            offset += stringData.position();
            var typesLength = countTypes - offset + stringsStart;
            types = slice(buffer1Main, offset, typesLength);
            offset += typesLength;
            if (countBlocks > 0) {
                blobSizes = slice(buffer1Main, offset, sizeUncompressed1 - offset);
            }
        }

        if (version >= 5) {
            var buffer2 = le(ByteBuffer.wrap(decompress(in, compression, sizeCompressed2, sizeUncompressed2)));
            var b2 = new Buffers();
            buffer = b2;
            var end = countObjectsB2 * 4;
            objectLengths = slice(buffer2, 0, end);
            offset = end;
            if (countBytes1b2 > 0) {
                b2.b1 = slice(buffer2, offset, countBytes1b2);
                offset += countBytes1b2;
            }
            if (countBytes2b2 > 0) {
                offset = align(offset, 2);
                b2.b2 = slice(buffer2, offset, countBytes2b2 * 2);
                offset += countBytes2b2 * 2;
            }
            if (countBytes4b2 > 0) {
                offset = align(offset, 4);
                b2.b4 = slice(buffer2, offset, countBytes4b2 * 4);
                offset += countBytes4b2 * 4;
            }
            if (countBytes8b2 > 0) {
                offset = align(offset, 8);
                b2.b8 = slice(buffer2, offset, countBytes8b2 * 8);
                offset += countBytes8b2 * 8;
            }
            types = slice(buffer2, offset, countTypes);
            offset += countTypes;
            if (countBlocks > 0) {
                blobSizes = slice(buffer2, offset, sizeUncompressed2 - offset);
            }
        }

        if (countBlocks > 0) {
            blobLengths = slice(blobSizes, 0, countBlocks * 4);
            if (blobSizes.getInt(countBlocks * 4) != TRAILER) {
                throw new IOException("KV3 blob length trailer mismatch");
            }
            if (sizeBinaryBlobs == 0) {
                blobs = ByteBuffer.allocate(0);
            } else if (compression == COMPRESSION_NONE) {
                blobs = le(ByteBuffer.wrap(readBytes(in, sizeBinaryBlobs)));
            } else if (compression == COMPRESSION_LZ4) {
                throw new IOException("LZ4-compressed KV3 binary blobs are not supported");
            } else if (version < 5) {
                blobs = le(buffer1.slice(sizeUncompressed1, sizeBinaryBlobs));
            } else {
                var sizeCompressedBlobs = sizeCompressedTotal - sizeCompressed1 - sizeCompressed2;
                blobs = le(ByteBuffer.wrap(decompress(in, compression, sizeCompressedBlobs, sizeBinaryBlobs)));
            }
        }

        var rootType = readType();
        return readValue(rootType);
    }

    private int readType() throws IOException {
        var t = types.get() & 0xFF;
        if ((t & 0x80) != 0) {
            t &= version >= 3 ? 0x3F : 0x7F;
            types.get();
        }
        return t;
    }

    private Object readValue(int type) throws IOException {
        var b = buffer;
        return switch (type) {
            case NULL -> null;
            case BOOLEAN_TRUE -> Boolean.TRUE;
            case BOOLEAN_FALSE -> Boolean.FALSE;
            case INT64_ZERO -> 0L;
            case INT64_ONE -> 1L;
            case DOUBLE_ZERO -> 0.0;
            case DOUBLE_ONE -> 1.0;
            case BOOLEAN -> b.b1.get() == 1;
            case INT32_AS_BYTE -> b.b1.get() & 0xFF;
            case INT16 -> b.b2.getShort();
            case UINT16 -> b.b2.getShort() & 0xFFFF;
            case INT32 -> b.b4.getInt();
            case UINT32 -> Integer.toUnsignedLong(b.b4.getInt());
            case FLOAT -> b.b4.getFloat();
            case INT64, UINT64 -> b.b8.getLong();
            case DOUBLE -> b.b8.getDouble();
            case STRING -> {
                var id = b.b4.getInt();
                yield id == -1 ? "" : strings[id];
            }
            case BINARY_BLOB -> {
                var length = blobLengths.getInt();
                var bytes = new byte[length];
                blobs.get(bytes);
                yield bytes;
            }
            case ARRAY -> {
                var length = b.b4.getInt();
                var list = new ArrayList<>(length);
                for (var i = 0; i < length; i++) {
                    list.add(readValue(readType()));
                }
                yield list;
            }
            case ARRAY_TYPED, ARRAY_TYPE_BYTE_LENGTH -> {
                var length = type == ARRAY_TYPE_BYTE_LENGTH ? b.b1.get() & 0xFF : b.b4.getInt();
                var subType = readType();
                var list = new ArrayList<>(length);
                for (var i = 0; i < length; i++) {
                    list.add(readValue(subType));
                }
                yield list;
            }
            case ARRAY_TYPE_AUXILIARY_BUFFER -> {
                var length = b.b1.get() & 0xFF;
                var subType = readType();
                var list = new ArrayList<>(length);
                var saved = buffer;
                buffer = auxiliary;
                auxiliary = saved;
                for (var i = 0; i < length; i++) {
                    list.add(readValue(subType));
                }
                auxiliary = buffer;
                buffer = saved;
                yield list;
            }
            case OBJECT -> {
                var length = version >= 5 ? objectLengths.getInt() : b.b4.getInt();
                var map = new LinkedHashMap<String, Object>();
                for (var i = 0; i < length; i++) {
                    var memberType = readType();
                    var nameId = buffer.b4.getInt();
                    map.put(nameId == -1 ? "" : strings[nameId], readValue(memberType));
                }
                yield map;
            }
            default -> throw new IOException("unknown KV3 type " + type);
        };
    }

    private static byte[] decompress(ByteBuffer in, int compression, int compressedSize, int uncompressedSize) throws IOException {
        if (compression == COMPRESSION_NONE) {
            return readBytes(in, uncompressedSize);
        }
        var input = readBytes(in, compressedSize);
        var output = new byte[uncompressedSize];
        var written = compression == COMPRESSION_LZ4
                ? new Lz4Decompressor().decompress(input, 0, input.length, output, 0, output.length)
                : new ZstdDecompressor().decompress(input, 0, input.length, output, 0, output.length);
        if (written != uncompressedSize) {
            throw new IOException("decompression: expected " + uncompressedSize + " bytes, got " + written);
        }
        return output;
    }

    private static byte[] readBytes(ByteBuffer in, int length) {
        var bytes = new byte[length];
        in.get(bytes);
        return bytes;
    }

    private static String readCString(ByteBuffer b) {
        var start = b.position();
        var end = start;
        while (b.get(end) != 0) {
            end++;
        }
        var bytes = new byte[end - start];
        b.get(bytes);
        b.get();
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static ByteBuffer slice(ByteBuffer b, int offset, int length) {
        return le(b.slice(offset, length));
    }

    private static ByteBuffer le(ByteBuffer b) {
        return b.order(ByteOrder.LITTLE_ENDIAN);
    }

    private static int align(int offset, int alignment) {
        return (offset + alignment - 1) & -alignment;
    }

}
