package infer;

import java.util.HashMap;

public final class InferGenerateManagement {
    private static volatile InferGenerateManagement instance;

    public static String PROJECT_PATH;
    private final HashMap<String, InferGenerateCode> generateDataByFilePath;

    private InferGenerateManagement(String projectPath) {
        PROJECT_PATH = projectPath;
        generateDataByFilePath = new HashMap<>();
    }

    public static InferGenerateManagement getInstance(String projectPath) {
        if (instance == null) {
            instance = new InferGenerateManagement(projectPath);
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
}
