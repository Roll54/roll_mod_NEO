package com.roll_54.roll_mod.data.datagen;

import com.roll_54.roll_mod.RollMod;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Finds hand-made source files in the {@code common} module's resources for datagen providers that read them. */
public final class DatagenSourcePaths {

    /**
     * Locates {@code common/src/main/resources/assets/<modid>/<relative>}. Datagen may launch from any
     * module's working directory (e.g. {@code :client:runData}), so several candidate roots relative to the
     * CWD are probed and the first existing one is picked rather than assuming a single-module layout.
     */
    public static Path locateAssets(Path relative) {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (cwd.getFileName() != null && cwd.getFileName().toString().equals("run")) {
            cwd = cwd.getParent();
        }
        Path tail = Path.of("src", "main", "resources", "assets", RollMod.MODID).resolve(relative);
        List<Path> candidates = List.of(
                cwd.resolve(tail),                                   // running from the module that holds the sources
                cwd.resolve("common").resolve(tail),                // running from the repo root
                cwd.resolveSibling("common").resolve(tail)          // running from a sibling module (client/server)
        );
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        // Fall back to the first candidate so a "missing file" error still names a sensible path.
        return candidates.get(0);
    }

    private DatagenSourcePaths() {
    }
}
