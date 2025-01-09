package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.example.gitManager.CollectedMergeDataByFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static infer.InferConstants.*;
import static infer.InferGenerateManagement.PROJECT_PATH;
import static org.example.utils.PathToString.fileSourceToString;

public class InferGenerate {
    private InferGenerateManagement generateManagement;

    public InferGenerate(String projectPath) {
        createInferPackage(projectPath);
        generateManagement = InferGenerateManagement.getInstance(projectPath);
    }

    public void generateInferCodeForEachCollectedMergeData(List<CollectedMergeDataByFile> collectedMergeDataByFiles) {
        for (CollectedMergeDataByFile collectedMergeData : collectedMergeDataByFiles) {
            InferParser inferParser = new InferParser();

            try {
                String filePath = collectedMergeData.getFilePath();
                String source = fileSourceToString(filePath);
                CompilationUnit compilationUnit = inferParser.getCompilationUnit(filePath, PROJECT_PATH, source);

                AST ast = compilationUnit.getAST();
                ASTRewrite rewriter = ASTRewrite.create(ast);
                InferGenerateCode inferGenerateCode = generateManagement.addGenerateData(filePath, compilationUnit, rewriter);

                inferGenerateCode.changeProgramToInferPackage(ast);

                InferVisitorHelper visitorHelper = new InferVisitorHelper(inferGenerateCode, collectedMergeData::getWhoChangedTheLine);
                InferVisitor inferVisitor = new InferVisitor(inferGenerateCode, visitorHelper);
                compilationUnit.accept(inferVisitor);

                inferGenerateCode.createInferClassFile(source);
                generateManagement.removeGenerateData(filePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
