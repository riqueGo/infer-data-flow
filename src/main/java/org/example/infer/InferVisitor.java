package org.example.infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.example.gitManager.InferCollectedMergeData;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node.getRightHandSide());
        rewriter.set(node, Assignment.RIGHT_HAND_SIDE_PROPERTY, inferWrapper, null);

        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation == null) { return super.visit(node); }

        IMethodBinding methodBinding = node.resolveMethodBinding();
        if (methodBinding == null) { return super.visit(node); }

        boolean isVoid = methodBinding.getReturnType().isPrimitive() && "void".equals(methodBinding.getReturnType().getName());

        if(isVoid) {
            List<Expression> newArguments = new ArrayList<>();
            for (Object argObj : node.arguments()) {
                if (argObj instanceof Expression argument) {
                    MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, argument);
                    newArguments.add(inferWrapper);
                }
            }

            ListRewrite argumentRewrite = rewriter.getListRewrite(node, MethodInvocation.ARGUMENTS_PROPERTY);
            for(int i = 0; i < newArguments.size(); i++) {
                argumentRewrite.remove((ASTNode) node.arguments().get(i), null);
                argumentRewrite.insertAt(newArguments.get(i), i, null);
            }
        } else {
            MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, node);
            rewriter.replace(node.getParent(), inferWrapper, null);
        }

        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        String nameMethodInvocation = getNameMethodInferWrapperInvocation(node);
        if (nameMethodInvocation == null) { return super.visit(node); }

        for (Object fragmentObj : node.fragments()) {
            if (fragmentObj instanceof VariableDeclarationFragment fragment) {
                Expression initializer = fragment.getInitializer();

                if (initializer != null) {
                    MethodInvocation inferWrapper = wrapInferMethodInvocation(node.getAST(), nameMethodInvocation, initializer);
                    rewriter.set(fragment, VariableDeclarationFragment.INITIALIZER_PROPERTY, inferWrapper, null);
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

    private MethodInvocation wrapInferMethodInvocation(AST ast, String nameMethodInvocation, Expression expression) {
        MethodInvocation methodInvocation = ast.newMethodInvocation();
        methodInvocation.setExpression(ast.newSimpleName(wrapperClassName));
        methodInvocation.setName(ast.newSimpleName(nameMethodInvocation));
        methodInvocation.arguments().add(ASTNode.copySubtree(ast, expression));
        return methodInvocation;
    }
}
