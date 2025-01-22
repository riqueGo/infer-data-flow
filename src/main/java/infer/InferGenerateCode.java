package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static infer.InferConstants.INFER_PACKAGE_NAME;
import static infer.InferConstants.INFER_PACKAGE_PATH;
import static infer.InferGenerateManagement.PROJECT_PATH;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.example.utils.PathToString.*;

public class InferGenerateCode {
    private String filePath;
    private String source;
    private boolean isCompilationActive;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;

    InferGenerateCode(String sourceFilePath) {
        String targetPath = PROJECT_PATH + INFER_PACKAGE_PATH;
        String fileName = getFileName(sourceFilePath);
        this.filePath = getPath(targetPath, fileName);

        copyFileToInferPackage(sourceFilePath);
        activeCompilation();
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

    public void changeProgramToInferPackage(AST ast) {
        PackageDeclaration oldPackage = compilationUnit.getPackage();
        String oldPackageName = oldPackage.getName().getFullyQualifiedName();

        if (!oldPackageName.equals(INFER_PACKAGE_NAME)) {
            PackageDeclaration newPackageDeclaration = ast.newPackageDeclaration();
            newPackageDeclaration.setName(ast.newName(INFER_PACKAGE_NAME));
            rewriter.set(compilationUnit, CompilationUnit.PACKAGE_PROPERTY, newPackageDeclaration, null);

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

            changeProgramToInferPackage(ast);

            isCompilationActive = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void copyFileToInferPackage(String sourceFilePath) {
        Path sourcePath = Path.of(sourceFilePath);
        Path targetPath = Path.of(filePath);

        try {
            Files.copy(sourcePath, targetPath, REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
