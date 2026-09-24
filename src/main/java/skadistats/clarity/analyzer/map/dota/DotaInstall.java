package skadistats.clarity.analyzer.map.dota;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Locates the {@code game/dota/maps} directory of a local Dota 2 installation.
 * The {@code DOTA2_DIR} environment variable (pointing at the "dota 2 beta" folder) takes precedence
 * over the Steam libraries found on the machine. On Windows, Steam's install location is read from
 * the registry.
 */
public final class DotaInstall {

    private static final String DOTA_DIR = "steamapps/common/dota 2 beta";
    private static final Pattern LIBRARY_PATH = Pattern.compile("\"path\"\\s+\"([^\"]+)\"");
    private static final Pattern REG_VALUE = Pattern.compile("^\\s*(\\S+)\\s+REG_(?:EXPAND_)?SZ\\s+(.+?)\\s*$", Pattern.MULTILINE);

    private DotaInstall() {}

    public static Optional<Path> findMapsDir() {
        var candidates = new ArrayList<Path>();
        var override = System.getenv("DOTA2_DIR");
        if (override != null && !override.isBlank()) {
            candidates.add(Path.of(override));
        }
        for (var library : steamLibraries(steamRoots())) {
            candidates.add(library.resolve(DOTA_DIR));
        }
        return candidates.stream()
                .map(d -> d.resolve("game/dota/maps"))
                .filter(Files::isDirectory)
                .findFirst();
    }

    private static List<Path> steamRoots() {
        var home = Path.of(System.getProperty("user.home"));
        var os = System.getProperty("os.name", "").toLowerCase();
        var roots = new ArrayList<Path>();
        if (os.startsWith("windows")) {
            registryValue("HKCU\\Software\\Valve\\Steam", "SteamPath").ifPresent(roots::add);
            registryValue("HKLM\\SOFTWARE\\WOW6432Node\\Valve\\Steam", "InstallPath").ifPresent(roots::add);
            registryValue("HKLM\\SOFTWARE\\Valve\\Steam", "InstallPath").ifPresent(roots::add);
            for (var env : List.of("ProgramFiles(x86)", "ProgramFiles")) {
                var dir = System.getenv(env);
                if (dir != null) roots.add(Path.of(dir, "Steam"));
            }
        } else if (os.startsWith("mac")) {
            roots.add(home.resolve("Library/Application Support/Steam"));
        } else {
            roots.add(home.resolve(".local/share/Steam"));
            roots.add(home.resolve(".steam/steam"));
            roots.add(home.resolve(".var/app/com.valvesoftware.Steam/.local/share/Steam"));
        }
        return roots;
    }

    static List<Path> steamLibraries(List<Path> roots) {
        var libraries = new LinkedHashSet<Path>();
        for (var root : roots) {
            if (!Files.isDirectory(root)) continue;
            libraries.add(root);
            for (var vdf : List.of(root.resolve("steamapps/libraryfolders.vdf"), root.resolve("config/libraryfolders.vdf"))) {
                if (!Files.isRegularFile(vdf)) continue;
                try {
                    libraries.addAll(parseLibraryFolders(Files.readString(vdf)));
                } catch (IOException ignored) {
                }
            }
        }
        return new ArrayList<>(libraries);
    }

    static List<Path> parseLibraryFolders(String vdf) {
        var result = new ArrayList<Path>();
        var m = LIBRARY_PATH.matcher(vdf);
        while (m.find()) {
            toPath(m.group(1).replace("\\\\", "\\")).ifPresent(result::add);
        }
        return result;
    }

    private static Optional<Path> registryValue(String key, String value) {
        try {
            var process = new ProcessBuilder("reg", "query", key, "/v", value)
                    .redirectErrorStream(true)
                    .start();
            var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
                return Optional.empty();
            }
            return parseRegQuery(output, value).flatMap(DotaInstall::toPath);
        } catch (IOException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    static Optional<String> parseRegQuery(String output, String value) {
        var m = REG_VALUE.matcher(output);
        while (m.find()) {
            if (m.group(1).equalsIgnoreCase(value)) {
                return Optional.of(m.group(2));
            }
        }
        return Optional.empty();
    }

    private static Optional<Path> toPath(String s) {
        try {
            return Optional.of(Path.of(s));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

}
