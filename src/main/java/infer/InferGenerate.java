package infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.example.gitManager.CollectedMergeDataByFile;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static infer.InferUtils.*;

public class InferGenerate {
    private List<CollectedMergeDataByFile> collectedMergeDataByFiles;
    private CompilationUnit compilationUnit;
    private ASTRewrite rewriter;

    public InferGenerate(List<CollectedMergeDataByFile> collectedMergeDataByFiles) {
        this.collectedMergeDataByFiles = collectedMergeDataByFiles;
    }

    public void generateInferCode(String projectPath) {
        createInferPackage(projectPath);


        for (CollectedMergeDataByFile collectedMergeData : collectedMergeDataByFiles) {
            InferParser inferParser = new InferParser();

            try {
                String source = new String(Files.readAllBytes(Path.of(collectedMergeData.getFilePath())));
                compilationUnit = inferParser.getCompilationUnit(collectedMergeData.getFilePath(), collectedMergeData.getProjectPath(), source);

                AST ast = compilationUnit.getAST();
                rewriter = ASTRewrite.create(ast);

                changeProgramToInferPackage(ast);

                InferVisitor inferVisitor = new InferVisitor(collectedMergeData, compilationUnit, rewriter);
                compilationUnit.accept(inferVisitor);

                createInferClassFile(source, projectPath + INFER_PACKAGE_PATH, collectedMergeData.getFileName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createInferPackage(String targetPath) {
        Path sourceDirPath = Path.of(".", INFER_PACKAGE_PATH);
        Path targetDirPath = Path.of(targetPath, INFER_PACKAGE_PATH);
        Path wrapperFilePath = sourceDirPath.resolve(WRAPPER_CLASS_NAME + ".java");

        try {
            if (Files.notExists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            if (Files.exists(wrapperFilePath)) {
                Path targetFile = targetDirPath.resolve(wrapperFilePath.getFileName());
                Files.copy(wrapperFilePath, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.println("Error creating Infer Package: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void createInferClassFile(String source, String targetPath, String fileName) {
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

    private void changeProgramToInferPackage(AST ast) {
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
}
