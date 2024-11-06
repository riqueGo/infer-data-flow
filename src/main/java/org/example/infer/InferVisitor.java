package org.example.infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.example.gitManager.CollectedMergeMethodData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class InferVisitor extends ASTVisitor {
    private CollectedMergeMethodData collectedMergeMethodData;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;
    private String className;

    public InferVisitor(CollectedMergeMethodData collectedMergeMethodData, CompilationUnit compilationUnit) {
        this.collectedMergeMethodData = collectedMergeMethodData;
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

        if (collectedMergeMethodData.getLeftAddedLines().contains(nodeLine)) {
            System.out.print("|" + nodeType + "|Left|" + nodeLine + "|" + node);
        }

        if (collectedMergeMethodData.getRightAddedLines().contains(nodeLine)) {
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

        Path targetFilePath = Paths.get(targetPath, className + ".java").toAbsolutePath();

        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            writer.write(document.get());
        } catch (IOException e) {
            System.err.println("Error writing to file " + targetFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
