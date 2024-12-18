package org.example.infer;

import org.eclipse.jdt.core.dom.*;
import org.example.gitManager.InferCollectedMergeData;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class InferParser {
    private InferCollectedMergeData inferCollectedMergeData;
    public static final String sourceProjectPath = "/src/main/java"; //TODO: This should be passed as program argument
    public static final String inferDependenciesPath = Path.of(sourceProjectPath, "inferDependencies").toString();

    public InferParser(InferCollectedMergeData inferCollectedMergeData) {
        this.inferCollectedMergeData = inferCollectedMergeData;
    }

    public void execute() throws IOException {
        String source = new String(Files.readAllBytes(Path.of(inferCollectedMergeData.getFilePath())));

        ASTParser parser = ASTParser.newParser(AST.JLS_Latest);

        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setResolveBindings(true);
        parser.setBindingsRecovery(true);

        String[] sourcepath = {Path.of(inferCollectedMergeData.getProjectPath(), sourceProjectPath).toString()};

        parser.setEnvironment(null, sourcepath, null, true);
        parser.setSource(source.toCharArray());
        parser.setUnitName(inferCollectedMergeData.getFileName());

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        InferVisitor inferVisitor = new InferVisitor(inferCollectedMergeData, cu);

        cu.accept(inferVisitor);
        inferVisitor.createInferClassFile(source, inferCollectedMergeData.getProjectPath() + inferDependenciesPath);
    }

    public static void createInferPackage(String targetPath) {
        Path sourceDirPath = Path.of(".", inferDependenciesPath);
        Path targetDirPath = Path.of(targetPath, inferDependenciesPath);

        try {
            if (Files.notExists(targetDirPath)) {
                Files.createDirectories(targetDirPath);
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirPath)) {
                for (Path file : stream) {
                    Path targetFile = targetDirPath.resolve(file.getFileName());
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        } catch (IOException e) {
            System.err.println("Error copying files: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
