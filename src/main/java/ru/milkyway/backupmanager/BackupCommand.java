package ru.milkyway.backupmanager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BackupCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public BackupCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("backupmanager.admin")) {
            sender.sendMessage("§cНедостаточно прав");
            return true;
        }

        if (!inProgress.compareAndSet(false, true)) {
            sender.sendMessage("§8[§bBackupManager§8] §cСоздание резервной копии уже выполняется");
            return true;
        }

        sender.sendMessage("§8[§bBackupManager§8] §7Запуск асинхронной архивации сборки...");

        Path root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        Path outputZip = root.resolve("backup-" + timestamp + ".zip");

        CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                BackupArchiver.ArchiveResult result = BackupArchiver.createArchive(root, outputZip);
                long zipSize = Files.size(outputZip);
                long archTime = System.currentTimeMillis() - start;

                sender.sendMessage("§8[§bBackupManager§8] §7Упаковано " + result.fileCount()
                        + " файлов (" + formatBytes(zipSize) + ") за " + (archTime / 1000.0) + " сек. Загрузка на Gofile...");

                long uploadStart = System.currentTimeMillis();
                String downloadUrl = GofileClient.upload(outputZip);
                long uploadTime = System.currentTimeMillis() - uploadStart;

                sender.sendMessage("§8[§bBackupManager§8] §aСборка готова: §f§n" + downloadUrl);
                sender.sendMessage("§8[§bBackupManager§8] §7Загружено за " + (uploadTime / 1000.0) + " сек. Временный архив удалён с диска");
            } catch (Exception e) {
                sender.sendMessage("§8[§bBackupManager§8] §cСбой при создании резервной копии: " + e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(outputZip);
                } catch (Exception ignored) {
                }
                inProgress.set(false);
            }
        });

        return true;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(bytes / Math.pow(1024, exp)) + " " + unit + "B";
    }
}
