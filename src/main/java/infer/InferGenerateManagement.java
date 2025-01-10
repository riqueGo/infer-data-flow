package infer;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;

import java.io.IOException;
import java.util.HashMap;

import static org.example.utils.PathToString.fileSourceToString;

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
        InferGenerateCode generateData = null;
        try {
            String source = fileSourceToString(filePath);

            InferParser inferParser = new InferParser();
            CompilationUnit compilationUnit = inferParser.getCompilationUnit(filePath, PROJECT_PATH, source);

            AST ast = compilationUnit.getAST();
            ASTRewrite rewriter = ASTRewrite.create(ast);

            generateData = new InferGenerateCode(filePath, source, compilationUnit, rewriter);
            generateDataByFilePath.put(filePath, generateData);

            generateData.changeProgramToInferPackage(ast);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return generateData;
    }

    public void removeGenerateData(String filePath) {
        generateDataByFilePath.remove(filePath);
    }

    public boolean containsGenerateData(String filePath) {
        return generateDataByFilePath.containsKey(filePath);
    }

    public InferGenerateCode getOrCreateGenerateData(String filePath) {
        if (generateDataByFilePath.containsKey(filePath)) {
            return generateDataByFilePath.get(filePath);
        }
        return addGenerateData(filePath);
    }
}
