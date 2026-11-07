package ru.milkyway.backupmanager;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BackupManager extends JavaPlugin {

    private BackupCommand backupCommand;

    @Override
    public void onEnable() {
        this.backupCommand = new BackupCommand(this);
        PluginCommand cmd = getCommand("backupmanager");
        if (cmd != null) {
            cmd.setExecutor(backupCommand);
        }
        getLogger().info("BackupManager v" + getPluginMeta().getVersion() + " успешно запущен");
    }

    @Override
    public void onDisable() {
        if (backupCommand != null) {
            backupCommand.cancelCurrentBackup();
        }
        getLogger().info("BackupManager выключен");
    }
}
