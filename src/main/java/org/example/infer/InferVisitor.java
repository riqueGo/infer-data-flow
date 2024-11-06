package org.example.infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.example.gitManager.InferCollectedMergeData;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class InferVisitor extends ASTVisitor {
    private InferCollectedMergeData inferCollectedMergeData;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;
    private String className;

    public InferVisitor(InferCollectedMergeData inferCollectedMergeData, CompilationUnit compilationUnit) {
        this.inferCollectedMergeData = inferCollectedMergeData;
        this.compilationUnit = compilationUnit;

        AST ast = compilationUnit.getAST();
        this.rewriter = ASTRewrite.create(ast);

        PackageDeclaration newPackageDeclaration = ast.newPackageDeclaration();
        newPackageDeclaration.setName(ast.newName("inferDependencies"));

        rewriter.set(compilationUnit, CompilationUnit.PACKAGE_PROPERTY, newPackageDeclaration, null);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        className = node.getName().getIdentifier();
        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        printVisitor(node);
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        printVisitor(node);
        return super.visit(node);
    }

    private void printVisitor(Statement node) {
        int nodeLine = compilationUnit.getLineNumber(node.getStartPosition());
        String nodeType = node.getClass().getSimpleName();

        if (inferCollectedMergeData.getLeftAddedLines().contains(nodeLine)) {
            System.out.print("|" + nodeType + "|Left|" + nodeLine + "|" + node);
        }

        if (inferCollectedMergeData.getRightAddedLines().contains(nodeLine)) {
            System.out.print("|" + nodeType + "|Right|" + nodeLine + "|" + node);
        }
    }

    public void createInferClassFile(String source, String targetPath) {
        Document document = new Document(source);
        TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
        } catch (Exception e) {
            System.err.println("Error applying edits: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Path targetFilePath = Path.of(targetPath, className + ".java");

        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            writer.write(document.get());
        } catch (IOException e) {
            System.err.println("Error writing to file " + targetFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
