package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathToString {

    public static String getFileName(String filePath) {return  Path.of(filePath).getFileName().toString(); }

    public static String getPath(String first, String... more) { return  Path.of(first, more).toString(); }

    public static String resolvePath(Path path, String other) { return  path.resolve(other).toString(); }

    public static String fileSourceToString(String filepath) throws IOException { return new String(Files.readAllBytes(Path.of(filepath))); }
}
