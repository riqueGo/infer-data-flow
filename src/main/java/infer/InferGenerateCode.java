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

import static infer.InferConstants.INFER_PACKAGE_NAME;
import static infer.InferConstants.INFER_PACKAGE_PATH;
import static infer.InferGenerateManagement.PROJECT_PATH;
import static org.example.utils.PathToString.getFileName;

public class InferGenerateCode {
    private String filePath;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;

    InferGenerateCode(String filePath, CompilationUnit compilationUnit, ASTRewrite rewriter) {
        this.filePath = filePath;
        this.compilationUnit = compilationUnit;
        this.rewriter = rewriter;
    }

    public String getFilePath() {
        return filePath;
    }

    public void createInferClassFile(String source) {
        String targetPath = PROJECT_PATH + INFER_PACKAGE_PATH;
        String fileName = getFileName(filePath);

        Document document = new Document(source);
        TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
        } catch (Exception e) {
            System.err.println("Error applying edits: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        Path targetFilePath = Path.of(targetPath, fileName);

        try (FileWriter writer = new FileWriter(targetFilePath.toFile())) {
            writer.write(document.get());
        } catch (IOException e) {
            System.err.println("Error writing to file " + targetFilePath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void changeProgramToInferPackage(AST ast) {
        PackageDeclaration oldPackage = compilationUnit.getPackage();
        String oldPackageName = oldPackage.getName().getFullyQualifiedName();

        PackageDeclaration newPackageDeclaration = ast.newPackageDeclaration();
        newPackageDeclaration.setName(ast.newName(INFER_PACKAGE_NAME));
        rewriter.set(compilationUnit, CompilationUnit.PACKAGE_PROPERTY, newPackageDeclaration, null);

        // Add an import statement for the old package
        if (!oldPackageName.equals(INFER_PACKAGE_NAME)) {
            ImportDeclaration importDeclaration = ast.newImportDeclaration();
            importDeclaration.setName(ast.newName(oldPackageName));
            importDeclaration.setOnDemand(true);

            rewriter.getListRewrite(compilationUnit, CompilationUnit.IMPORTS_PROPERTY).insertLast(importDeclaration, null);
        }
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
}
