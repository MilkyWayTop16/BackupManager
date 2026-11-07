package ru.milkyway.backupmanager;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public final class BackupFilter {

    private static final Set<String> EXCLUDED_ROOT_DIRS = Set.of(
            "world",
            "world_nether",
            "world_the_end",
            "world2",
            "alabygapolitex",
            "logs",
            "libraries",
            "cache",
            ".paper-nms",
            ".git",
            ".idea"
    );

    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
            "plugins/coreprotect",
            "plugins/customdiscs/musicdata",
            "plugins/inventoryrollbackplus/backups",
            "plugins/squaremap/web/tiles",
            "plugins/.plugmanreloaded-backups"
    );

    private static final Set<String> EXCLUDED_SUBDIRS = Set.of(
            "region",
            "entities",
            "poi"
    );

    public static boolean shouldSkip(Path root, Path target) {
        Path relative = root.relativize(target);
        String pathStr = relative.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        if (pathStr.isEmpty()) {
            return false;
        }

        String firstSegment = pathStr.contains("/") ? pathStr.substring(0, pathStr.indexOf('/')) : pathStr;
        if (EXCLUDED_ROOT_DIRS.contains(firstSegment)
                || firstSegment.startsWith("_backup")
                || firstSegment.startsWith(".backup")) {
            return true;
        }

        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (pathStr.equals(prefix) || pathStr.startsWith(prefix + "/")) {
                return true;
            }
        }

        for (Path part : relative) {
            String seg = part.toString().toLowerCase(Locale.ROOT);
            if (EXCLUDED_SUBDIRS.contains(seg)) {
                return true;
            }
        }

        String fileName = target.getFileName() != null ? target.getFileName().toString().toLowerCase(Locale.ROOT) : "";
        if (fileName.endsWith(".mca")
                || fileName.endsWith(".db")
                || fileName.endsWith(".sqlite")
                || fileName.endsWith(".sqlite3")
                || fileName.endsWith(".hprof")
                || fileName.endsWith(".dump")
                || fileName.endsWith(".log.gz")
                || (fileName.startsWith("backup-") && fileName.endsWith(".zip"))) {
            return true;
        }

        return false;
    }
}
