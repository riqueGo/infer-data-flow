package infer;

import org.example.gitManager.CollectedMergeDataByFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.function.Function;

import static infer.InferConstants.*;
import static infer.InferGenerateManagement.PROJECT_PATH;

public class InferGenerate {
    private final InferGenerateManagement generateManagement;

    public InferGenerate(String projectPath) {
        generateManagement = InferGenerateManagement.getInstance(projectPath);
    }

    public void generateInferCodeForEachCollectedMergeData(List<CollectedMergeDataByFile> collectedMergeDataByFiles) {
        createInferPackage(PROJECT_PATH);
        for (CollectedMergeDataByFile collectedMergeData : collectedMergeDataByFiles) {
            collectedMergeData.setFilePath("/home/rique/Documents/research/infer-data-flow-test/src/main/java/org/example/ClassC.java");
            String filePath = collectedMergeData.getFilePath();
            InferGenerateCode inferGenerateCode = generateManagement.addGenerateData(filePath);

            if (inferGenerateCode == null) continue;

            InferVisitorHelper visitorHelper = new InferVisitorHelper(inferGenerateCode, collectedMergeData::getWhoChangedTheLine, INTERPROCEDURAL_DEPTH);
            InferVisitor inferVisitor = new InferVisitor(inferGenerateCode, visitorHelper);

            inferGenerateCode.accept(inferVisitor);
            inferGenerateCode.createInferClassFile();
            generateManagement.removeGenerateData(filePath);
        }
    }

    public void generateInferInterproceduralMethodCode(String filePath, String methodVisiting, String developer, int depth) {
        if (depth < 0 || Files.notExists(Path.of(filePath))) return;

        boolean isFirstFileVisiting = !generateManagement.containsGenerateData(filePath);
        InferGenerateCode inferGenerateCode = generateManagement.getOrCreateGenerateData(filePath);

        if (inferGenerateCode == null) return;

        InferInterproceduralMethodVisitorHelper visitorHelper = new InferInterproceduralMethodVisitorHelper(inferGenerateCode, depth, methodVisiting, developer);
        InferInterproceduralMethodVisitor inferVisitor = new InferInterproceduralMethodVisitor(visitorHelper);

        inferGenerateCode.accept(inferVisitor);

        if (isFirstFileVisiting) {
            inferGenerateCode.createInferClassFile();
            generateManagement.removeGenerateData(filePath);
        }
    }

    private void createInferPackage(String targetPath) {
        Path sourceDirPath = Path.of(WORKING_DIRECTORY, INFER_PACKAGE_PATH);
        Path targetDirPath = Path.of(targetPath, INFER_PACKAGE_PATH);
        Path wrapperFilePath = sourceDirPath.resolve(WRAPPER_CLASS_NAME + ".java");

        try {
            if (Files.notExists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            if (Files.exists(wrapperFilePath)) {
                Path targetFile = targetDirPath.resolve(wrapperFilePath.getFileName());
                Files.copy(wrapperFilePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error creating Infer Package: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
