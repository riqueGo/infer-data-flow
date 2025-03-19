package infer;

import org.example.gitManager.CollectedMergeDataByFile;
import java.util.ArrayList;
import java.util.List;
import static org.example.utils.FileUtils.isFileExists;

public class InferGenerate {
    private final InferGenerateManagement generateManagement;

    public InferGenerate() {
        generateManagement = InferGenerateManagement.getInstance();
    }

    public InferGenerate(List<CollectedMergeDataByFile> collectedMergeDataByFiles) {
        generateManagement = InferGenerateManagement.getInstance();
        setSourcesProject(collectedMergeDataByFiles);
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
        if (depth < 0) return;

        filePath = generateManagement.getAbsoluteFilePath(filePath);
        if(filePath == null) { return; } //Doesn't exist this filePath

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

    private void setSourcesProject(List<CollectedMergeDataByFile> collectedMergeDataByFiles) {
        List<String> filePaths = new ArrayList<>();
        collectedMergeDataByFiles.forEach(collectedMergeDataByFile -> {filePaths.add(collectedMergeDataByFile.getFilePath());});
        generateManagement.setSourcesProject(filePaths);
    }
}
