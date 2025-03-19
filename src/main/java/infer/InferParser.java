package infer;

import org.eclipse.jdt.core.dom.*;
import static infer.InferGenerateManagement.SOURCES_PROJECT;
import static org.example.utils.PathToString.getFileName;

public class InferParser {

    public CompilationUnit getCompilationUnit(String filePath, String sourceFile) {
        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        parser.setEnvironment(null, SOURCES_PROJECT, null, true);
        parser.setSource(sourceFile.toCharArray());
        parser.setUnitName(getFileName(filePath));

        return (CompilationUnit) parser.createAST(null);
    }
}
