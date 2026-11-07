package ru.milkyway.backupmanager;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupFilterTest {

    private final Path root = Path.of("C:/server");

    @Test
    void testCoreProtectDatabaseExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/CoreProtect/database.db")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/CoreProtect/config.yml")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("database.db")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/test/storage.sqlite")));
    }

    @Test
    void testWorldsExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("world")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("world/level.dat")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("world/region/r.0.0.mca")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("world_nether/entities/c.0.0.dat")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("world_the_end/poi/c.0.0.dat")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("alabygapolitex/region/r.1.1.mca")));
    }

    @Test
    void testCustomDiscsMusicExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/CustomDiscs/musicdata")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/CustomDiscs/musicdata/track.ogg")));
    }

    @Test
    void testInventoryRollbackBackupsExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/InventoryRollbackPlus/backups")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/InventoryRollbackPlus/backups/deaths/user.yml")));
    }

    @Test
    void testSquaremapTilesExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/squaremap/web/tiles")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("plugins/squaremap/web/tiles/0/0/0.png")));
    }

    @Test
    void testLogsAndDumpsExcluded() {
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("logs/latest.log.gz")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("logs/2026-11-01-1.log.gz")));
        assertTrue(BackupFilter.shouldSkip(root, root.resolve("dump.hprof")));
    }

    @Test
    void testValuableServerFilesKept() {
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("server.properties")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("purpur.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("spigot.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("config/leaf-global.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("config/paper-global.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/PlugManReloaded.jar")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/DeluxeMenus/config.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/DeluxeMenus/gui_menus/main.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/Skript/scripts/races.sk")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/CustomDiscs/config.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/InventoryRollbackPlus/config.yml")));
        assertFalse(BackupFilter.shouldSkip(root, root.resolve("plugins/squaremap/config.yml")));
    }
}
