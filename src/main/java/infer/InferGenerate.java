package infer;

import org.example.gitManager.CollectedMergeDataByFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static infer.InferConstants.*;
import static org.example.utils.FileUtils.isFileExists;
import static org.example.utils.FileUtils.moveDirectory;

public class InferGenerate {
    private final InferGenerateManagement generateManagement;

    public InferGenerate(String projectPath) {
        generateManagement = InferGenerateManagement.getInstance(projectPath);
    }

    public void generateInferCodeForEachCollectedMergeData(List<CollectedMergeDataByFile> collectedMergeDataByFiles, int depth) {
        for (CollectedMergeDataByFile collectedMergeData : collectedMergeDataByFiles) {
            String filePath = collectedMergeData.getFilePath();
            if(!isFileExists(filePath)) {
                System.out.println("File not found: " + filePath);
                continue;
            }
            InferGenerateCode inferGenerateCode = generateManagement.getGenerateData(filePath);

            if (!inferGenerateCode.isActive()) {
                inferGenerateCode.activeCompilation();
            }

            InferVisitorHelper visitorHelper = new InferVisitorHelper(inferGenerateCode, collectedMergeData::getWhoChangedTheLine, depth);
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
}
