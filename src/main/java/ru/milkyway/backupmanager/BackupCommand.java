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
    private volatile String lastStatus = "Резервных копий еще не создавалось";
    private volatile String lastUrl = null;
    private volatile CompletableFuture<?> currentFuture = null;
    private volatile Path currentZip = null;

    public BackupCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void cancelCurrentBackup() {
        CompletableFuture<?> future = currentFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
        Path zip = currentZip;
        if (zip != null) {
            try {
                Files.deleteIfExists(zip);
            } catch (Exception ignored) {
            }
        }
        inProgress.set(false);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("backupmanager.admin")) {
            sender.sendMessage("§cНедостаточно прав");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("status")) {
            if (inProgress.get()) {
                sender.sendMessage("§8[§bBackupManager§8] §eСоздание и выгрузка копии в процессе");
            } else if (lastUrl != null) {
                sender.sendMessage("§8[§bBackupManager§8] §aПоследняя копия готова: §f§n" + lastUrl);
                sender.sendMessage("§8[§bBackupManager§8] §7Статус: " + lastStatus);
            } else {
                sender.sendMessage("§8[§bBackupManager§8] §7" + lastStatus);
            }
            return true;
        }

        if (!inProgress.compareAndSet(false, true)) {
            sender.sendMessage("§8[§bBackupManager§8] §cСоздание резервной копии уже выполняется");
            sender.sendMessage("§8[§bBackupManager§8] §7Используйте /" + label + " status для проверки");
            return true;
        }

        lastStatus = "Архивация и выгрузка запущены";
        sender.sendMessage("§8[§bBackupManager§8] §7Запуск асинхронной архивации сборки");
        plugin.getLogger().info("[BackupManager] Запуск асинхронной архивации сборки");

        Path root = plugin.getServer().getWorldContainer().toPath().toAbsolutePath().normalize();
        String timestamp = LocalDateTime.now().format(FORMATTER);
        Path outputZip = root.resolve("backup-" + timestamp + ".zip");
        this.currentZip = outputZip;

        this.currentFuture = CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                BackupArchiver.ArchiveResult result = BackupArchiver.createArchive(root, outputZip);
                long zipSize = Files.size(outputZip);
                long archTime = System.currentTimeMillis() - start;

                String archMsg = "Упаковано " + result.fileCount()
                        + " файлов (" + formatBytes(zipSize) + ") за " + (archTime / 1000.0) + " сек. Загрузка на Gofile";
                sender.sendMessage("§8[§bBackupManager§8] §7" + archMsg);
                plugin.getLogger().info("[BackupManager] " + archMsg);

                long uploadStart = System.currentTimeMillis();
                String downloadUrl = GofileClient.upload(outputZip);
                long uploadTime = System.currentTimeMillis() - uploadStart;

                lastUrl = downloadUrl;
                lastStatus = "Загружено за " + (uploadTime / 1000.0) + " сек (" + formatBytes(zipSize) + ")";

                String successMsg = "Сборка готова: " + downloadUrl;
                sender.sendMessage("§8[§bBackupManager§8] §a" + successMsg);
                sender.sendMessage("§8[§bBackupManager§8] §7Загружено за " + (uploadTime / 1000.0) + " сек. Временный архив удалён с диска");
                plugin.getLogger().info("[BackupManager] " + successMsg);
                plugin.getLogger().info("[BackupManager] " + lastStatus);
            } catch (Exception e) {
                lastStatus = "Ошибка: " + e.getMessage();
                sender.sendMessage("§8[§bBackupManager§8] §cСбой при создании резервной копии: " + e.getMessage());
                plugin.getLogger().warning("[BackupManager] Сбой при создании резервной копии: " + e.getMessage());
            } finally {
                try {
                    Files.deleteIfExists(outputZip);
                } catch (Exception ignored) {
                }
                currentZip = null;
                currentFuture = null;
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
