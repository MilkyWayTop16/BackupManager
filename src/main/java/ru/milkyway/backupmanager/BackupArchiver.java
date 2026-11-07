package ru.milkyway.backupmanager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class BackupArchiver {

    public record ArchiveResult(Path zipFile, int fileCount, long totalBytes) {}

    public static ArchiveResult createArchive(Path root, Path outputZip) throws IOException {
        int[] count = new int[1];
        long[] bytes = new long[1];

        if (Files.exists(outputZip)) {
            Files.delete(outputZip);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(outputZip), 65536))) {
            zos.setLevel(Deflater.BEST_SPEED);
            byte[] buffer = new byte[65536];

            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (Thread.currentThread().isInterrupted()) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (BackupFilter.shouldSkip(root, dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Thread.currentThread().isInterrupted()) {
                        return FileVisitResult.TERMINATE;
                    }
                    if (file.equals(outputZip) || BackupFilter.shouldSkip(root, file)) {
                        return FileVisitResult.CONTINUE;
                    }

                    String entryName = root.relativize(file).toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(entryName);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);

                    try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file), 65536)) {
                        int read;
                        while ((read = bis.read(buffer)) != -1) {
                            if (Thread.currentThread().isInterrupted()) {
                                return FileVisitResult.TERMINATE;
                            }
                            zos.write(buffer, 0, read);
                            bytes[0] += read;
                        }
                    }

                    zos.closeEntry();
                    count[0]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });

            if (Thread.currentThread().isInterrupted()) {
                throw new IOException("Архивация прервана");
            }

            zos.finish();
        }

        return new ArchiveResult(outputZip, count[0], bytes[0]);
    }
}
