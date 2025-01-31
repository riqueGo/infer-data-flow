package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;

import static infer.InferConstants.*;
import static infer.InferGenerateManagement.PROJECT_PATH;
import static org.example.utils.PathToString.*;

public class InferGenerateCode {
    private String filePath;
    private String source;
    private boolean isCompilationActive;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;
    private final HashSet<String> alreadyInterproceduralVisitingBy;

    InferGenerateCode(String filePath) {
        this.filePath = filePath;
        activeCompilation();
        addInferWrapperImport();

        alreadyInterproceduralVisitingBy = new HashSet<>();
    }

    public String getFilePath() {
        return filePath;
    }

    public void rewriteFile() {
        Document document = new Document(source);
        TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
        } catch (Exception e) {
            System.err.println("Error applying edits: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Path targetFilePath = Path.of(filePath);

        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            writer.write(document.get());
        } catch (IOException e) {
            System.err.println("Error writing to file " + targetFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addInferWrapperImport() {
        AST ast = compilationUnit.getAST();
        ImportDeclaration importDeclaration = ast.newImportDeclaration();
        importDeclaration.setName(ast.newName(INFER_PACKAGE_NAME + "." + WRAPPER_CLASS_NAME));
        rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY).insertLast(importDeclaration, null);
    }

    public int getLineNumber(int position) {
        return compilationUnit.getLineNumber(position);
    }

    public final void rewriterSet(ASTNode node, StructuralPropertyDescriptor property, Object value, TextEditGroup editGroup) {
        rewriter.set(node, property, value, editGroup);
    }

    public final void rewriterReplace(ASTNode node, ASTNode replacement, TextEditGroup editGroup) {
        rewriter.replace(node, replacement, editGroup);
    }

    public final ListRewrite getListRewrite(ASTNode node, ChildListPropertyDescriptor property) {
        return rewriter.getListRewrite(node, property);
    }

    public final void accept(ASTVisitor visitor) {
        compilationUnit.accept(visitor);
    }

    public boolean isActive() {
        return isCompilationActive;
    }

    public void desactiveCompilation() {
        this.isCompilationActive = false;
        this.source = null;
        this.compilationUnit = null;
        this.rewriter = null;
    }

    public void activeCompilation() {
        try {
            source = fileSourceToString(filePath);

            InferParser inferParser = new InferParser();
            compilationUnit = inferParser.getCompilationUnit(filePath, PROJECT_PATH, source);

            AST ast = compilationUnit.getAST();
            rewriter = ASTRewrite.create(ast);

            isCompilationActive = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasDevAlreadyInterproceduralVisited(String developer, String methodDeclaration) {
        return alreadyInterproceduralVisitingBy.contains(developer + " - " + methodDeclaration);
    }

    public void addDevAndMethodDeclaration(String developer, String methodDeclaration) {
        alreadyInterproceduralVisitingBy.add(developer + " - " + methodDeclaration);
    }
}
