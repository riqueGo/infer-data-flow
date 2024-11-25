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
    private final String packageName = "inferDependencies";
    private final String wrapperClassName = "InferWrapper";

    public InferVisitor(InferCollectedMergeData inferCollectedMergeData, CompilationUnit compilationUnit) {
        this.inferCollectedMergeData = inferCollectedMergeData;
        this.compilationUnit = compilationUnit;

        AST ast = compilationUnit.getAST();
        this.rewriter = ASTRewrite.create(ast);

        PackageDeclaration newPackageDeclaration = ast.newPackageDeclaration();
        newPackageDeclaration.setName(ast.newName(packageName));

        rewriter.set(compilationUnit, CompilationUnit.PACKAGE_PROPERTY, newPackageDeclaration, null);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        inferCollectedMergeData.addClassName(packageName + "." + node.getName().getIdentifier());
        return super.visit(node);
    }

    @Override
    public boolean visit(Assignment node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation == null) {
            return super.visit(node);
        }

        AST ast = node.getAST();

        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(wrapperClassName));
        methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));

        methodInvocation.arguments().add(ASTNode.copySubtree(ast, node.getRightHandSide()));

        rewriter.set(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, methodInvocation, null);

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation == null) { return super.visit(node); }

        AST ast = node.getAST();

        LambdaExpression lambdaExpression = ast.newLambdaExpression();
        lambdaExpression.setBody(ASTNode.copySubtree(ast, node));

        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(wrapperClassName));
        methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));
        methodInvocation.arguments().add(lambdaExpression);

        rewriter.replace(node.getParent(), ast.newExpressionStatement(methodInvocation), null);

        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation == null) { return super.visit(node); }

        AST ast = node.getAST();

        for (Object fragmentObj : node.fragments()) {
            if (fragmentObj instanceof VariableDeclarationFragment fragment) {
                Expression initializer = fragment.getInitializer();

                if (initializer != null) {
                    MethodInvocation methodInvocation = ast.newMethodInvocation();
                    methodInvocation.setExpression(ast.newSimpleName(wrapperClassName));
                    methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));
                    methodInvocation.arguments().add(ASTNode.copySubtree(ast, initializer));

                    rewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, methodInvocation, null);
                }
            }
        }
        return super.visit(node);
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

        Path targetFilePath = Path.of(targetPath, inferCollectedMergeData.getFileName());

        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            writer.write(document.get());
        } catch (IOException e) {
            System.err.println("Error writing to file " + targetFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String getNameMethodInferWrapperInvocation(ASTNode node) {
        int nodeLine = compilationUnit.getLineNumber(node.getStartPosition());
        if (inferCollectedMergeData.getLeftAddedLines().contains(nodeLine)) {
            return "left";
        } else if (inferCollectedMergeData.getRightAddedLines().contains(nodeLine)) {
            return "right";
        }
        return null;
    }
}
