package org.example.infer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.example.gitManager.CollectedMergeMethodData;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class InferParser {
    private CollectedMergeMethodData collectedMergeMethodData;

    public InferParser(CollectedMergeMethodData collectedMergeMethodData) {
        this.collectedMergeMethodData = collectedMergeMethodData;
    }

    public void execute() throws IOException {
        String source = new String(Files.readAllBytes(Paths.get(collectedMergeMethodData.getFilePath())));

        createInferPackage(collectedMergeMethodData.getProjectPath());

        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);
        parser.setSource(source.toCharArray());

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        InferVisitor inferVisitor = new InferVisitor(collectedMergeMethodData, cu);

        cu.accept(inferVisitor);
        inferVisitor.createInferClassFile(source, collectedMergeMethodData.getProjectPath() + "src/main/java/inferDependencies");
    }

    private char[] readFileToCharArray(File file) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            char[] contents = new char[(int) file.length()];
            reader.read(contents);
            return contents;
        }
    }

    private void createInferPackage(String targetPath) {
        try {
            Path targetDirPath = Path.of(targetPath + "/src/main/java/inferDependencies");
            Path sourceFilePath = Path.of("./src/main/java/inferDependencies/InferWrapper.java");
            Path targetFilePath = Path.of(targetPath + "src/main/java/inferDependencies/InferWrapper.java");

            if(Files.notExists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            Files.copy(sourceFilePath, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
