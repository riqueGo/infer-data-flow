package infer;

import org.example.gitManager.CollectedMergeDataByFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
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
            String filePath = collectedMergeData.getFilePath();
            InferGenerateCode inferGenerateCode = generateManagement.getGenerateData(filePath);

            InferVisitorHelper visitorHelper = new InferVisitorHelper(inferGenerateCode, collectedMergeData::getWhoChangedTheLine, INTERPROCEDURAL_DEPTH);
            InferVisitor inferVisitor = new InferVisitor(inferGenerateCode, visitorHelper);

            inferGenerateCode.accept(inferVisitor);
            inferGenerateCode.rewriteFile();
            inferGenerateCode.desactiveCompilation();
        }
    }

    public void generateInferInterproceduralCode(String filePath, String classVisiting, String methodDeclarationName, String developer, int depth) {
        if (depth < 0 || Files.notExists(Path.of(filePath))) return;

        boolean firstVisiting = !generateManagement.hasCompilationActive(filePath);
        InferGenerateCode inferGenerateCode = generateManagement.getGenerateData(filePath);

        if (inferGenerateCode.hasDevAlreadyInterproceduralVisited(developer, methodDeclarationName)) { return; }
        inferGenerateCode.addDevAndMethodDeclaration(developer, methodDeclarationName);

        if (!inferGenerateCode.isActive()) {
            inferGenerateCode.activeCompilation();
        }

        InferVisitorHelper visitorHelper = new InferVisitorHelper(inferGenerateCode, x -> developer, depth, classVisiting, methodDeclarationName);
        InferVisitor inferVisitor = new InferVisitor(inferGenerateCode, visitorHelper);

        inferGenerateCode.accept(inferVisitor);

        if (firstVisiting) {
            inferGenerateCode.rewriteFile();
            inferGenerateCode.desactiveCompilation();
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
