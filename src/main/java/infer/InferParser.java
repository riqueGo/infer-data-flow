package infer;

import org.eclipse.jdt.core.dom.*;
import static infer.InferConstants.SOURCE_PROJECT_PATH;
import static org.example.utils.PathToString.getFileName;
import static org.example.utils.PathToString.getPath;

public class InferParser {

    public CompilationUnit getCompilationUnit(String filePath, String projectPath, String sourceFile) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        String[] sourcepath = { getPath(projectPath) };

        parser.setEnvironment(null, sourcepath, null, true);
        parser.setSource(sourceFile.toCharArray());
        parser.setUnitName(getFileName(filePath));

        return (CompilationUnit) parser.createAST(null);
    }
}
