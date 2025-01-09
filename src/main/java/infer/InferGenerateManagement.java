package infer;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.util.HashMap;

public final class InferGenerateManagement {
    private static volatile InferGenerateManagement instance;

    public static String PROJECT_PATH;
    private HashMap<String, InferGenerateCode> generateDataByFilePath;

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

    public String getProjectPath() {
        return PROJECT_PATH;
    }

    public InferGenerateCode addGenerateData(String filePath, CompilationUnit compilationUnit, ASTRewrite rewriter) {
        InferGenerateCode generateData = new InferGenerateCode(filePath, compilationUnit, rewriter);
        generateDataByFilePath.put(filePath, generateData);
        return generateData;
    }

    public void removeGenerateData(String filePath) {
        generateDataByFilePath.remove(filePath);
    }

}
