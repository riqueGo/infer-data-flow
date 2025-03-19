package infer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.example.utils.PathToString.getPath;

public final class InferGenerateManagement {
    private static volatile InferGenerateManagement instance;

    public static String[] SOURCES_PROJECT;
    private final HashMap<String, InferGenerateCode> generateDataByFilePath;

    private InferGenerateManagement() {
        generateDataByFilePath = new HashMap<>();
    }

    public static InferGenerateManagement getInstance() {
        if (instance == null) {
            instance = new InferGenerateManagement();
        }
        return instance;
    }

    public InferGenerateCode addGenerateData(String filePath)  {
        InferGenerateCode generateData = new InferGenerateCode(filePath);
        generateDataByFilePath.put(filePath, generateData);
        return generateData;
    }

    public boolean hasCompilationActive(String filePath) {
        return generateDataByFilePath.containsKey(filePath) && generateDataByFilePath.get(filePath).isActive();
    }

    public InferGenerateCode getGenerateData(String filePath) {
        return generateDataByFilePath.containsKey(filePath) ? generateDataByFilePath.get(filePath) : addGenerateData(filePath);
    }

    public void setSourcesProject(List<String> filePaths) {
        Set<String> sourcePaths = new HashSet<>();

        for (String filePath : filePaths) {
            File file = new File(filePath);

            while (file != null) {
                if (file.getPath().endsWith("src" + File.separator + "main" + File.separator + "java")) {
                    sourcePaths.add(file.getPath());
                    break;  // Stop once the source path is found
                }
                file = file.getParentFile();
            }
        }
        SOURCES_PROJECT = sourcePaths.toArray(new String[sourcePaths.size()]);
    }

    public String getAbsoluteFilePath(String filePath) {
        for (String sourcePath : SOURCES_PROJECT) {
            if(Files.exists(Path.of(sourcePath, filePath))) {
                return getPath(sourcePath, filePath);
            }
        }
        return null;
    }
}
