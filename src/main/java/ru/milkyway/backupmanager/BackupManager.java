package ru.milkyway.backupmanager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackupManager extends JavaPlugin {

    @Override
    public void onEnable() {
        PluginCommand cmd = getCommand("backupmanager");
        if (cmd != null) {
            cmd.setExecutor(new BackupCommand(this));
        }
        getLogger().info("BackupManager v" + getPluginMeta().getVersion() + " успешно запущен");
    }

    @Override
    public void onDisable() {
        getLogger().info("BackupManager выключен");
    }
}
