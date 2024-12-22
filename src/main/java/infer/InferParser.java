package infer;

import org.eclipse.jdt.core.dom.*;
import java.nio.file.Path;
import static infer.InferUtils.SOURCE_PROJECT_PATH;

public class InferParser {

    public CompilationUnit getCompilationUnit(String filePath, String projectPath, String sourceFile) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        String[] sourcepath = {Path.of(projectPath, SOURCE_PROJECT_PATH).toString()};

        parser.setEnvironment(null, sourcepath, null, true);
        parser.setSource(sourceFile.toCharArray());
        parser.setUnitName(Path.of(filePath).getFileName().toString());

        return (CompilationUnit) parser.createAST(null);
    }
}
