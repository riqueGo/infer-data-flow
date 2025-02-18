package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class FileUtils {

    public static void renameFile(String path, String oldName, String newFileName) {
        Path reportFile = Path.of(path, oldName);
        Path renamedReportFile = Path.of(path, newFileName);

        try {
            if (Files.exists(reportFile)) {
                Files.move(reportFile, renamedReportFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void moveDirectory(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) {
            Files.createDirectories(source);
        }

        if (!Files.exists(destination)) {
            Files.createDirectories(destination);
        }

        try(Stream<Path> entries = Files.walk(destination).sorted(Comparator.reverseOrder())) {
            entries.forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete file: " + path, e);
                        }
                    });
        }

        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}
